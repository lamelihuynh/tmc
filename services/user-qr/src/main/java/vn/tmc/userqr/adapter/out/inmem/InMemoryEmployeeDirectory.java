package vn.tmc.userqr.adapter.out.inmem;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.userqr.application.port.out.IEmployeeDirectory;

/** Eligibility held in memory, loaded from the shared dev seed or updated by HR/tests. */
public class InMemoryEmployeeDirectory implements IEmployeeDirectory {
  private final Map<UUID, Eligibility> rows = new ConcurrentHashMap<>();

  public InMemoryEmployeeDirectory() {}

  public InMemoryEmployeeDirectory(Collection<Eligibility> initial) {
    initial.forEach(this::save);
  }

  /** Newer replaces older for the same employee. */
  public void put(Eligibility e) {
    save(e);
  }

  @Override
  public Optional<Eligibility> findById(UUID employeeUuid) {
    return employeeUuid == null ? Optional.empty() : Optional.ofNullable(rows.get(employeeUuid));
  }

  @Override
  public void save(Eligibility e) {
    rows.merge(
        e.employeeUuid(), e, (old, neu) -> neu.updatedAt().isBefore(old.updatedAt()) ? old : neu);
  }

  @Override
  public void saveIfAbsent(Eligibility e) {
    rows.putIfAbsent(e.employeeUuid(), e);
  }

  @Override
  public List<Eligibility> changedSince(Instant since) {
    return rows.values().stream()
        .filter(e -> e.updatedAt().isAfter(since))
        .sorted(
            Comparator.comparing(Eligibility::updatedAt).thenComparing(Eligibility::employeeUuid))
        .toList();
  }
}
