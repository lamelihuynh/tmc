package vn.tmc.userqr.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Delta pulled by Edge Agent and kiosks so they can verify and revoke offline (document.md 16.2 row
 * 3): device keys and employee eligibility changed after {@code since}.
 */
public interface ISyncBundle {

  record DeviceKey(
      String deviceId, UUID employeeUuid, String publicKeyX509Base64, boolean revoked) {}

  /** docs/api/user-qr.openapi.yaml EmployeeEligibility. status is ACTIVE or INACTIVE. */
  record Employee(UUID employeeUuid, String status, List<String> factories, Instant updatedAt) {}

  record Bundle(Instant generatedAt, List<DeviceKey> deviceKeys, List<Employee> employees) {
    public Bundle(Instant generatedAt, List<DeviceKey> deviceKeys) {
      this(generatedAt, deviceKeys, List.of());
    }

    public Bundle {
      deviceKeys = deviceKeys == null ? List.of() : List.copyOf(deviceKeys);
      employees = employees == null ? List.of() : List.copyOf(employees);
    }
  }

  Bundle changedSince(Instant since);
}
