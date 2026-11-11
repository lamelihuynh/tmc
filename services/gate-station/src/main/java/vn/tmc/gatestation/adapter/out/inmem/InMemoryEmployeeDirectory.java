package vn.tmc.gatestation.adapter.out.inmem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.gatestation.application.port.out.IEmployeeDirectory;
import vn.tmc.gatestation.domain.EmployeeAccess;

/** Fixed eligibility list loaded once (from application.yml in the scaffold). */
public class InMemoryEmployeeDirectory implements IEmployeeDirectory {
  private final Map<UUID, EmployeeAccess> employees = new HashMap<>();

  public InMemoryEmployeeDirectory(List<EmployeeAccess> employees) {
    employees.forEach(e -> this.employees.put(e.employeeUuid(), e));
  }

  @Override
  public Optional<EmployeeAccess> find(UUID employeeUuid) {
    return Optional.ofNullable(employees.get(employeeUuid));
  }
}
