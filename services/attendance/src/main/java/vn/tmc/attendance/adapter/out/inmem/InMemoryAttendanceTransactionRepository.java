package vn.tmc.attendance.adapter.out.inmem;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.domain.AttendanceTransaction;
import vn.tmc.attendance.domain.Direction;

/** Process-memory attendance records, keyed by attempt id like the table's primary key. */
public class InMemoryAttendanceTransactionRepository implements IAttendanceTransactionRepository {
  private final Map<UUID, AttendanceTransaction> rows = new ConcurrentHashMap<>();

  @Override
  public boolean saveIfAbsent(AttendanceTransaction transaction) {
    return rows.putIfAbsent(transaction.attemptId(), transaction) == null;
  }

  @Override
  public Optional<Direction> lastDirectionBefore(UUID employeeUuid, Instant at) {
    return rows.values().stream()
        .filter(t -> t.employeeUuid().equals(employeeUuid) && t.occurredAt().isBefore(at))
        .max(Comparator.comparing(AttendanceTransaction::occurredAt))
        .map(AttendanceTransaction::direction);
  }

  /** For tests. */
  public List<AttendanceTransaction> all() {
    return rows.values().stream()
        .sorted(Comparator.comparing(AttendanceTransaction::occurredAt))
        .toList();
  }
}
