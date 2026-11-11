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

  /** Direction of the employee's latest counted pass strictly before {@code at}. */
  Optional<Direction> lastDirectionBefore(UUID employeeUuid, Instant at);
}
