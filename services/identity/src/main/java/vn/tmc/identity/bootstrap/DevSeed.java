package vn.tmc.identity.bootstrap;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Dev accounts. The employees MUST equal docs/api/fixtures/employee-seed.json (the single source of
 * the employee seed, also used by user-qr and gate-station); EmployeeSeedContractTest enforces it.
 * nv003 is INACTIVE in the seed: login still works here, the gate answers EMPLOYEE_INACTIVE.
 */
final class DevSeed {
  private DevSeed() {}

  static final String DEV_PASSWORD = "dev-only-change-me";

  record Employee(String username, UUID employeeUuid, Set<String> factories) {}

  static final List<Employee> EMPLOYEES =
      List.of(
          new Employee("nv001", uuid(1), Set.of("BD", "LA")),
          new Employee("nv002", uuid(2), Set.of("BD")),
          new Employee("nv003", uuid(3), Set.of("BD")),
          new Employee("nv004", uuid(4), Set.of("BD", "LA")),
          new Employee("nv005", uuid(5), Set.of("BD", "LA")));

  private static UUID uuid(int n) {
    return UUID.fromString(String.format("0192a1c0-0000-7000-8000-%012d", n));
  }
}
