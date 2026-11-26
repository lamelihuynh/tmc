package vn.tmc.attendance.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.attendance.domain.AttendanceTransaction;
import vn.tmc.attendance.domain.Direction;

/** Driven port: the canonical attendance records (table attendance_transactions, ADR-03). */
public interface IAttendanceTransactionRepository {
  /**
   * @return true when the record was stored, false when one already exists for the same attempt
   */
  boolean saveIfAbsent(AttendanceTransaction transaction);

  /** The immutable original record; corrections are stored separately. */
  Optional<AttendanceTransaction> findByAttemptId(UUID attemptId);

  /**
   * The employee's latest counted pass strictly before {@code at}, using each pass's latest
   * correction for both ordering and the cutoff (B-05, B-06).
   */
  Optional<AttendanceTransaction> latestBefore(UUID employeeUuid, Instant at);

  /** Direction of the employee's latest counted pass strictly before {@code at}. */
  default Optional<Direction> lastDirectionBefore(UUID employeeUuid, Instant at) {
    return latestBefore(employeeUuid, at).map(AttendanceTransaction::direction);
  }
}
