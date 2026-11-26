package vn.tmc.attendance.application.usecase;

import java.time.temporal.ChronoUnit;
import vn.tmc.attendance.application.port.in.AttemptNotFoundException;
import vn.tmc.attendance.application.port.in.IManualAttendanceVerification;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.application.port.out.ITransactionRunner;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttendanceTransaction;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Rbac;
import vn.tmc.platform.security.Role;

/**
 * Converts an HR-verified manual pass into one counted record and its outbox messages. The
 * aggregate lock and transaction protect retries, simultaneous verifiers and subsequent B-06
 * corrections. This use case has no turnstile port and cannot send another opening command.
 */
public class VerifyManualAttendanceUseCase implements IManualAttendanceVerification {
  private final IAttemptRepository attempts;
  private final IAttendanceTransactionRepository transactions;
  private final IOutboxRepository outbox;
  private final ITransactionRunner tx;
  private final AttendanceEventFactory events;
  private final IClock clock;

  public VerifyManualAttendanceUseCase(
      IAttemptRepository attempts,
      IAttendanceTransactionRepository transactions,
      IOutboxRepository outbox,
      ITransactionRunner tx,
      AttendanceEventFactory events,
      IClock clock) {
    this.attempts = attempts;
    this.transactions = transactions;
    this.outbox = outbox;
    this.tx = tx;
    this.events = events;
    this.clock = clock;
  }

  @Override
  public VerificationResult verify(Principal caller, VerifyManualAttendanceCommand command) {
    Rbac.requireRole(caller, Role.HR_STAFF);
    if (caller.subject() == null) {
      throw new AccessDeniedException("authenticated subject is required");
    }
    if (command == null || command.attemptId() == null) {
      throw new IllegalArgumentException("attemptId is required");
    }
    return tx.inTransaction(
        () -> {
          var attempt =
              attempts
                  .findByIdForUpdate(command.attemptId())
                  .orElseThrow(
                      () ->
                          caller.scope() != null && caller.scope().allFactories()
                              ? new AttemptNotFoundException(command.attemptId())
                              : new AccessDeniedException("attempt not available for caller"));
          Rbac.requireFactory(caller, attempt.factoryCode());
          var changed =
              attempt.verifyManual(
                  caller.subject(), command.reason(), clock.now().truncatedTo(ChronoUnit.MICROS));
          if (!changed) {
            return result(attempt, true);
          }

          var inserted =
              transactions.saveIfAbsent(
                  new AttendanceTransaction(
                      attempt.attemptId(),
                      attempt.employeeUuid(),
                      attempt.factoryCode(),
                      attempt.gateId(),
                      attempt.direction(),
                      attempt.scannedAt(),
                      attempt.source()));
          if (!inserted) {
            throw new IllegalStateException("unverified manual attempt already has attendance");
          }
          attempts.save(attempt);
          outbox.add(events.attendanceConfirmed(attempt, command.correlationId()));
          outbox.add(
              events.hrAuditRecorded(
                  attempt,
                  attempt.version(),
                  "MANUAL_ATTENDANCE_VERIFIED",
                  caller.subject(),
                  command.reason(),
                  attempt.attemptId(),
                  command.correlationId()));
          return result(attempt, false);
        });
  }

  private static VerificationResult result(AccessAttempt attempt, boolean idempotentReplay) {
    return new VerificationResult(
        attempt.attemptId(),
        attempt.manualVerifiedBy(),
        attempt.manualVerificationReason(),
        attempt.manualVerifiedAt(),
        attempt.version(),
        idempotentReplay);
  }
}
