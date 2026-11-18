package vn.tmc.gatestation.adapter.out.inmem;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.gatestation.application.port.out.IEmployeeDirectoryCache;
import vn.tmc.gatestation.domain.EmployeeAccess;

/**
 * Eligibility in process memory: seeded from application.yml, then updated from the sync bundle.
 * Losing it on restart is fine: the seed and the next sync (since EPOCH) rebuild it.
 */
public class InMemoryEmployeeDirectory implements IEmployeeDirectoryCache {
  private final Map<UUID, EmployeeAccess> employees = new ConcurrentHashMap<>();

  public InMemoryEmployeeDirectory(List<EmployeeAccess> employees) {
    upsertAll(employees);
  }

  @Override
  public Optional<EmployeeAccess> find(UUID employeeUuid) {
    return Optional.ofNullable(employees.get(employeeUuid));
  }

  @Override
  public void upsertAll(Collection<EmployeeAccess> changed) {
    changed.forEach(e -> employees.put(e.employeeUuid(), e));
  }
}
