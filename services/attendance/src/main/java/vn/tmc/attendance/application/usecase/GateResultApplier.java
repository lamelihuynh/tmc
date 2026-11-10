package vn.tmc.attendance.application.usecase;

import vn.tmc.attendance.application.port.in.GateResult;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.attendance.domain.AttendanceTransaction;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Single place that turns a turnstile outcome into state, attendance record and outbox events, so
 * the online path and the reconcile path cannot drift apart. Must be called inside a local
 * transaction (ADR-04).
 */
class GateResultApplier {
  private final IAttemptRepository attempts;
  private final IAttendanceTransactionRepository transactions;
  private final IOutboxRepository outbox;
  private final AttendanceEventFactory events;
  private final IClock clock;

  GateResultApplier(
      IAttemptRepository attempts,
      IAttendanceTransactionRepository transactions,
      IOutboxRepository outbox,
      AttendanceEventFactory events,
      IClock clock) {
    this.attempts = attempts;
    this.transactions = transactions;
    this.outbox = outbox;
    this.events = events;
    this.clock = clock;
  }

  /**
   * @param failureReason used only when result is ERROR
   */
  void apply(
      AccessAttempt attempt, GateResult result, ReasonCode failureReason, String correlationId) {
    var now = clock.now();
    switch (result) {
      case OPENED -> {
        attempt.gateOpened(now);
        outbox.add(events.gateResultRecorded(attempt, GateResult.OPENED, correlationId));
        attempt.commit(now);
        if (attempt.source() == AttemptSource.MANUAL) {
          // Manual opens wait for verification and are not counted automatically (document.md
          // 12.6).
          outbox.add(
              events.auditRecorded(attempt, "MANUAL_OPEN_PENDING_VERIFICATION", correlationId));
          break;
        }
        transactions.saveIfAbsent(
            new AttendanceTransaction(
                attempt.attemptId(),
                attempt.employeeUuid(),
                attempt.factoryCode(),
                attempt.gateId(),
                attempt.direction(),
                attempt.scannedAt(),
                attempt.source()));
        outbox.add(events.attendanceConfirmed(attempt, correlationId));
      }
      case ERROR -> {
        attempt.gateFailed(failureReason, now);
        outbox.add(events.gateResultRecorded(attempt, GateResult.ERROR, correlationId));
      }
      case UNKNOWN -> {
        if (attempt.status() == AttemptStatus.GATE_UNKNOWN) {
          return; // already unknown, nothing new to record
        }
        attempt.gateUnknown(now);
        outbox.add(events.gateResultRecorded(attempt, GateResult.UNKNOWN, correlationId));
      }
    }
    attempts.save(attempt);
  }
}
