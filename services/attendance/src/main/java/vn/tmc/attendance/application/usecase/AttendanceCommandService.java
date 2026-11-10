package vn.tmc.attendance.application.usecase;

import java.util.UUID;
import vn.tmc.attendance.application.port.in.AttemptNotFoundException;
import vn.tmc.attendance.application.port.in.AttemptView;
import vn.tmc.attendance.application.port.in.GateResult;
import vn.tmc.attendance.application.port.in.IAttendanceCommand;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.application.port.out.ITransactionRunner;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.ReasonCode;

/** Online write path of ADR-13 steps 4 and 6. Each public method is one local transaction. */
public class AttendanceCommandService implements IAttendanceCommand {
  private final IAttemptRepository attempts;
  private final IOutboxRepository outbox;
  private final ITransactionRunner tx;
  private final AttendanceEventFactory events;
  private final GateResultApplier applier;
  private final IClock clock;

  public AttendanceCommandService(
      IAttemptRepository attempts,
      IAttendanceTransactionRepository transactions,
      IOutboxRepository outbox,
      ITransactionRunner tx,
      AttendanceEventFactory events,
      IClock clock) {
    this.attempts = attempts;
    this.outbox = outbox;
    this.tx = tx;
    this.events = events;
    this.clock = clock;
    this.applier = new GateResultApplier(attempts, transactions, outbox, events, clock);
  }

  @Override
  public AttemptView recordAttempt(RecordAttemptCommand c) {
    return tx.inTransaction(
        () -> {
          var existing = attempts.findById(c.attemptId());
          if (existing.isPresent()) {
            return AttemptView.of(existing.get(), true);
          }
          var now = clock.now();
          var attempt =
              AccessAttempt.validated(
                  c.attemptId(),
                  c.employeeUuid(),
                  c.gateId(),
                  c.factoryCode(),
                  c.direction(),
                  c.jti(),
                  c.source(),
                  c.scannedAt(),
                  now);
          attempt.reserve(now);
          attempts.save(attempt);
          outbox.add(events.accessAttemptRecorded(attempt, c.correlationId()));
          return AttemptView.of(attempt, false);
        });
  }

  @Override
  public AttemptView recordDenied(RecordDeniedCommand c) {
    return tx.inTransaction(
        () -> {
          var existing = attempts.findById(c.attemptId());
          if (existing.isPresent()) {
            return AttemptView.of(existing.get(), true);
          }
          var now = clock.now();
          // Denials reuse the same aggregate so retries get the same answer (P1) and audit sees
          // them.
          var attempt =
              AccessAttempt.validated(
                  c.attemptId(),
                  c.employeeUuid(),
                  c.gateId(),
                  c.factoryCode(),
                  c.direction(),
                  c.jti(),
                  vn.tmc.attendance.domain.AttemptSource.ONLINE,
                  c.scannedAt(),
                  now);
          attempt.deny(c.reasonCode(), now);
          attempts.save(attempt);
          outbox.add(events.auditRecorded(attempt, "ACCESS_DENIED", c.correlationId()));
          return AttemptView.of(attempt, false);
        });
  }

  @Override
  public AttemptView markGateCommanded(UUID attemptId) {
    return tx.inTransaction(
        () -> {
          var attempt = load(attemptId);
          if (attempt.status() != AttemptStatus.RESERVED) {
            // already commanded, finished or denied: idempotent replay, never re-arm the gate
            return AttemptView.of(attempt, true);
          }
          attempt.commandGate(clock.now());
          attempts.save(attempt);
          return AttemptView.of(attempt, false);
        });
  }

  @Override
  public AttemptView recordGateResult(UUID attemptId, GateResult result, String correlationId) {
    return tx.inTransaction(
        () -> {
          var attempt = load(attemptId);
          if (attempt.status().isTerminal()) {
            return AttemptView.of(attempt, true);
          }
          applier.apply(attempt, result, ReasonCode.GATE_HARDWARE_ERROR, correlationId);
          return AttemptView.of(attempt, false);
        });
  }

  private AccessAttempt load(UUID attemptId) {
    return attempts.findById(attemptId).orElseThrow(() -> new AttemptNotFoundException(attemptId));
  }
}
