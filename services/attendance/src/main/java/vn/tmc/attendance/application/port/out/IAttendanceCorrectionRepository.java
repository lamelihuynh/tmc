package vn.tmc.attendance.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.attendance.domain.AttendanceCorrection;

/**
 * Driven port: HR corrections (table attendance_corrections, migration V2). Append only: a newer
 * correction is a new revision, an existing one is never changed.
 */
public interface IAttendanceCorrectionRepository {

  /** Globally unique idempotency key, including keys already used for a different pass. */
  Optional<AttendanceCorrection> findById(UUID correctionId);

  /**
   * @return false when this (originalAttemptId, revision) already exists: a concurrent correction
   *     won, reload and retry with the next revision
   */
  boolean add(AttendanceCorrection correction);

  /** Corrections of one counted pass, oldest revision first. */
  List<AttendanceCorrection> forAttempt(UUID originalAttemptId);

  /** The correction in force, if any. */
  default Optional<AttendanceCorrection> latest(UUID originalAttemptId) {
    var all = forAttempt(originalAttemptId);
    return all.isEmpty() ? Optional.empty() : Optional.of(all.get(all.size() - 1));
  }
}
