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

  /**
   * Dev-only RFC 4648 Base32 TOTP secrets for MFA-protected staff accounts (C-05). Loaded only when
   * {@code tmc.identity.mfa.dev-secrets-enabled=true} (profiles {@code local} and {@code inmem}).
   * Never used on the default profile.
   */
  static final String DEV_ADMIN_TOTP_SECRET = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

  static final String DEV_HR_TOTP_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

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
