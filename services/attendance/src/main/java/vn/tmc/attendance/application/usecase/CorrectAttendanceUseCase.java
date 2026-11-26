package vn.tmc.attendance.application.usecase;

import java.time.temporal.ChronoUnit;
import java.util.Objects;
import vn.tmc.attendance.application.port.in.AttemptNotFoundException;
import vn.tmc.attendance.application.port.in.AttendanceNotCountedException;
import vn.tmc.attendance.application.port.in.CorrectionConflictException;
import vn.tmc.attendance.application.port.in.IAttendanceCorrection;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IAttendanceCorrectionRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.application.port.out.ITransactionRunner;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttendanceCorrection;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Rbac;
import vn.tmc.platform.security.Role;

/**
 * FR-26: keep the original attendance immutable and append a revision together with both its
 * business event and HR audit in one transaction (ADR-04, ADR-08).
 */
public class CorrectAttendanceUseCase implements IAttendanceCorrection {
  private final IAttemptRepository attempts;
  private final IAttendanceTransactionRepository transactions;
  private final IAttendanceCorrectionRepository corrections;
  private final IOutboxRepository outbox;
  private final ITransactionRunner tx;
  private final AttendanceEventFactory events;
  private final IClock clock;

  public CorrectAttendanceUseCase(
      IAttemptRepository attempts,
      IAttendanceTransactionRepository transactions,
      IAttendanceCorrectionRepository corrections,
      IOutboxRepository outbox,
      ITransactionRunner tx,
      AttendanceEventFactory events,
      IClock clock) {
    this.attempts = attempts;
    this.transactions = transactions;
    this.corrections = corrections;
    this.outbox = outbox;
    this.tx = tx;
    this.events = events;
    this.clock = clock;
  }

  @Override
  public CorrectionView correct(Principal caller, CorrectAttendanceCommand command) {
    Rbac.requireRole(caller, Role.HR_STAFF);
    if (caller.subject() == null) {
      throw new AccessDeniedException("authenticated subject is required");
    }
    Objects.requireNonNull(command, "command");
    return tx.inTransaction(
        () -> {
          // Serialize all changes to this pass, including manual verification, across service
          // instances. A database row lock, not a process-local lock, protects revision order.
          var attempt =
              attempts
                  .findByIdForUpdate(command.originalAttemptId())
                  .orElseThrow(() -> missingAttempt(caller, command));
          Rbac.requireFactory(caller, attempt.factoryCode());
          if (!attempt.countsAsAttendance()) {
            throw new AttendanceNotCountedException();
          }
          var original =
              transactions
                  .findByAttemptId(attempt.attemptId())
                  .orElseThrow(AttendanceNotCountedException::new);

          var existing = corrections.findById(command.correctionId());
          if (existing.isPresent()) {
            return replay(existing.get(), command, caller, attempt);
          }

          var previous = corrections.latest(attempt.attemptId());
          var correction =
              AttendanceCorrection.of(
                  command.correctionId(),
                  original,
                  previous.map(c -> c.revision() + 1).orElse(1),
                  previous.map(AttendanceCorrection::direction).orElse(original.direction()),
                  previous.map(AttendanceCorrection::scannedAt).orElse(original.occurredAt()),
                  command.direction(),
                  command.scannedAt(),
                  command.reason(),
                  caller.subject(),
                  clock.now().truncatedTo(ChronoUnit.MICROS));

          if (!corrections.add(correction)) {
            // A key used concurrently for another pass is still a conflict, not a successful
            // replay. ON CONFLICT leaves the transaction usable for this lookup.
            var winner =
                corrections
                    .findById(command.correctionId())
                    .orElseThrow(CorrectionConflictException::new);
            return replay(winner, command, caller, attempt);
          }

          long version =
              AttendanceEventFactory.correctedVersion(attempt.version(), correction.revision());
          outbox.add(
              events.attendanceCorrected(
                  original, attempt.version(), correction, command.correlationId()));
          outbox.add(
              events.hrAuditRecorded(
                  attempt,
                  version,
                  "ATTENDANCE_CORRECTED",
                  caller.subject(),
                  correction.reason(),
                  correction.correctionId(),
                  command.correlationId()));
          return new CorrectionView(correction, false, version);
        });
  }

  private CorrectionView replay(
      AttendanceCorrection existing,
      CorrectAttendanceCommand command,
      Principal caller,
      AccessAttempt attempt) {
    if (!existing.originalAttemptId().equals(command.originalAttemptId())
        || existing.direction() != command.direction()
        || !existing.scannedAt().equals(command.scannedAt())
        || !existing.reason().equals(command.reason())
        || !existing.correctedBy().equals(caller.subject())) {
      throw new CorrectionConflictException();
    }
    return new CorrectionView(
        existing,
        true,
        AttendanceEventFactory.correctedVersion(attempt.version(), existing.revision()));
  }

  private RuntimeException missingAttempt(Principal caller, CorrectAttendanceCommand command) {
    // A scoped caller must not distinguish an unknown identifier from one outside their scope.
    if (caller.scope() == null || !caller.scope().allFactories()) {
      return new AccessDeniedException("attempt not available for caller");
    }
    return new AttemptNotFoundException(command.originalAttemptId());
  }
}
