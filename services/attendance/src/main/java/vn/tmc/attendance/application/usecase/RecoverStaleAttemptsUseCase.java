package vn.tmc.attendance.application.usecase;

import vn.tmc.attendance.application.port.in.GateResult;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.application.port.out.ITransactionRunner;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Crash recovery (ADR-11 section 4). After a restart or a lost reservation the edge finds attempts
 * that stopped midway and moves them to a safe state. It never resends a gate command.
 */
public class RecoverStaleAttemptsUseCase {
  private final IAttemptRepository attempts;
  private final IOutboxRepository outbox;
  private final ITransactionRunner tx;
  private final AttendanceEventFactory events;
  private final GateResultApplier applier;
  private final RecoveryPolicy policy;
  private final IClock clock;

  public RecoverStaleAttemptsUseCase(
      IAttemptRepository attempts,
      IAttendanceTransactionRepository transactions,
      IOutboxRepository outbox,
      ITransactionRunner tx,
      AttendanceEventFactory events,
      RecoveryPolicy policy,
      IClock clock) {
    this.attempts = attempts;
    this.outbox = outbox;
    this.tx = tx;
    this.events = events;
    this.policy = policy;
    this.clock = clock;
    this.applier = new GateResultApplier(attempts, transactions, outbox, events, clock);
  }

  public record Report(int releasedReservations, int movedToUnknown) {}

  public Report run() {
    int released = 0;
    int unknown = 0;
    var now = clock.now();

    // P2: reserved but the command never started, so nothing physical happened: release.
    var staleReserved =
        attempts.findByStatusUpdatedBefore(
            AttemptStatus.RESERVED, now.minus(policy.reservedTtl()), policy.batchSize());
    for (var a : staleReserved) {
      released +=
          tx.inTransaction(
              () -> {
                a.deny(ReasonCode.SYSTEM_RETRY, clock.now());
                attempts.save(a);
                outbox.add(events.auditRecorded(a, "RESERVATION_RELEASED", "recovery"));
                return 1;
              });
    }

    // P3/P4: command was (or may have been) sent and no result was recorded: treat as unknown.
    var staleCommanded =
        attempts.findByStatusUpdatedBefore(
            AttemptStatus.GATE_COMMANDED, now.minus(policy.commandGrace()), policy.batchSize());
    for (var a : staleCommanded) {
      unknown +=
          tx.inTransaction(
              () -> {
                applier.apply(a, GateResult.UNKNOWN, null, "recovery");
                return 1;
              });
    }
    return new Report(released, unknown);
  }
}
