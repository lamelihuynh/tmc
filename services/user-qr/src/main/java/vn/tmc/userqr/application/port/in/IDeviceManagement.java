package vn.tmc.userqr.application.port.in;

import java.util.UUID;
import vn.tmc.platform.security.Principal;

public interface IDeviceManagement {

  record RegisteredDevice(String deviceId) {}

  /** Employee registers (or replaces) their own phone. The previous ACTIVE device is revoked. */
  RegisteredDevice register(Principal caller, UUID employeeUuid, String publicKeyX509Base64);

  /** Employee revokes their own device. */
  void revokeOwn(Principal caller, String deviceId);

  /**
   * HR Staff or System Admin revokes whatever device the employee currently has (lost phone,
   * UC-02).
   */
  void revokeForEmployee(Principal caller, UUID employeeUuid);
}
