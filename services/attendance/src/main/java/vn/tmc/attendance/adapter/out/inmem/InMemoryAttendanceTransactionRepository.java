package vn.tmc.attendance.adapter.out.inmem;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.attendance.application.port.out.IAttendanceCorrectionRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.domain.AttendanceTransaction;

/** Process-memory attendance records, keyed by attempt id like the table's primary key. */
public class InMemoryAttendanceTransactionRepository implements IAttendanceTransactionRepository {
  private final Map<UUID, AttendanceTransaction> rows = new ConcurrentHashMap<>();
  private final IAttendanceCorrectionRepository corrections;

  public InMemoryAttendanceTransactionRepository() {
    this(new InMemoryAttendanceCorrectionRepository());
  }

  public InMemoryAttendanceTransactionRepository(IAttendanceCorrectionRepository corrections) {
    this.corrections = corrections;
  }

  @Override
  public boolean saveIfAbsent(AttendanceTransaction transaction) {
    return rows.putIfAbsent(transaction.attemptId(), transaction) == null;
  }

  @Override
  public Optional<AttendanceTransaction> findByAttemptId(UUID attemptId) {
    return Optional.ofNullable(rows.get(attemptId));
  }

  @Override
  public Optional<AttendanceTransaction> latestBefore(UUID employeeUuid, Instant at) {
    return rows.values().stream()
        .filter(t -> t.employeeUuid().equals(employeeUuid))
        .map(this::effective)
        .filter(t -> t.occurredAt().isBefore(at))
        .max(
            Comparator.comparing(AttendanceTransaction::occurredAt)
                .thenComparing(t -> t.attemptId().toString()));
  }

  private AttendanceTransaction effective(AttendanceTransaction original) {
    return corrections
        .latest(original.attemptId())
        .map(
            c ->
                new AttendanceTransaction(
                    original.attemptId(),
                    original.employeeUuid(),
                    original.factoryCode(),
                    original.gateId(),
                    c.direction(),
                    c.scannedAt(),
                    original.source()))
        .orElse(original);
  }

  /** For tests: immutable originals, before applying correction overlays. */
  public List<AttendanceTransaction> all() {
    return rows.values().stream()
        .sorted(Comparator.comparing(AttendanceTransaction::occurredAt))
        .toList();
  }
}
