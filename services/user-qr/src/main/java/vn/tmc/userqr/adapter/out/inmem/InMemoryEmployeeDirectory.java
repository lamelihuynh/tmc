package vn.tmc.userqr.adapter.out.inmem;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.userqr.application.port.out.IEmployeeDirectory;

/** Eligibility held in memory, loaded from configuration (dev seed) or set by tests. */
public class InMemoryEmployeeDirectory implements IEmployeeDirectory {
  private final Map<UUID, Eligibility> rows = new ConcurrentHashMap<>();

  public InMemoryEmployeeDirectory(Collection<Eligibility> initial) {
    initial.forEach(this::put);
  }

  /** Newer replaces older for the same employee. */
  public void put(Eligibility e) {
    rows.merge(
        e.employeeUuid(), e, (old, neu) -> neu.updatedAt().isBefore(old.updatedAt()) ? old : neu);
  }

  @Override
  public List<Eligibility> changedSince(Instant since) {
    return rows.values().stream()
        .filter(e -> e.updatedAt().isAfter(since))
        .sorted((a, b) -> a.employeeUuid().compareTo(b.employeeUuid()))
        .toList();
  }
}
