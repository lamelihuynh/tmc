package vn.tmc.attendance.adapter.out.inmem;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttemptStatus;

/**
 * Process-memory attempts. Rows are copied on the way in and out, like a database: changing a
 * loaded aggregate has no effect until it is saved.
 */
public class InMemoryAttemptRepository implements IAttemptRepository {
  private final Map<UUID, AccessAttempt> rows = new ConcurrentHashMap<>();

  @Override
  public Optional<AccessAttempt> findById(UUID attemptId) {
    return Optional.ofNullable(rows.get(attemptId)).map(InMemoryAttemptRepository::copy);
  }

  @Override
  public void save(AccessAttempt attempt) {
    rows.put(attempt.attemptId(), copy(attempt));
  }

  @Override
  public boolean existsOtherWithJti(String jti, UUID exceptAttemptId) {
    return rows.values().stream()
        .anyMatch(a -> jti.equals(a.jti()) && !a.attemptId().equals(exceptAttemptId));
  }

  @Override
  public List<AccessAttempt> findByStatus(AttemptStatus status, int limit) {
    return rows.values().stream()
        .filter(a -> a.status() == status)
        .sorted(Comparator.comparing(AccessAttempt::updatedAt))
        .limit(limit)
        .map(InMemoryAttemptRepository::copy)
        .toList();
  }

  @Override
  public List<AccessAttempt> findByStatusUpdatedBefore(
      AttemptStatus status, Instant cutoff, int limit) {
    return rows.values().stream()
        .filter(a -> a.status() == status && a.updatedAt().isBefore(cutoff))
        .sorted(Comparator.comparing(AccessAttempt::updatedAt))
        .limit(limit)
        .map(InMemoryAttemptRepository::copy)
        .toList();
  }

  @Override
  public List<AccessAttempt> findAnomalies(
      String factoryCode, Instant from, Instant to, int limit) {
    boolean hasFactory = factoryCode != null && !factoryCode.isBlank();
    return rows.values().stream()
        .filter(a -> a.anomaly() != null)
        .filter(a -> !hasFactory || factoryCode.equals(a.factoryCode()))
        .filter(a -> from == null || !a.scannedAt().isBefore(from))
        .filter(a -> to == null || !a.scannedAt().isAfter(to))
        .sorted(
            Comparator.comparing(AccessAttempt::scannedAt).thenComparing(AccessAttempt::attemptId))
        .limit(limit)
        .map(InMemoryAttemptRepository::copy)
        .toList();
  }

  @Override
  public long countByStatus(AttemptStatus status) {
    return rows.values().stream().filter(a -> a.status() == status).count();
  }

  private static AccessAttempt copy(AccessAttempt a) {
    return AccessAttempt.restore(
        a.attemptId(),
        a.employeeUuid(),
        a.gateId(),
        a.factoryCode(),
        a.direction(),
        a.jti(),
        a.source(),
        a.scannedAt(),
        a.status(),
        a.reasonCode(),
        a.anomaly(),
        a.updatedAt(),
        a.version());
  }
}
