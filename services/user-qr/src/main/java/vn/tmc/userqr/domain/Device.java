package vn.tmc.userqr.domain;

import java.security.PublicKey;
import java.time.Instant;
import java.util.UUID;

/**
 * A registered phone and its public key (ADR-12 device binding). The private key never leaves the
 * phone. deviceId is also the "kid" of every token the phone signs. At most one ACTIVE device per
 * employee.
 */
public record Device(
    String deviceId,
    UUID employeeUuid,
    PublicKey publicKey,
    DeviceStatus status,
    Instant registeredAt,
    Instant updatedAt) {

  public static Device register(String deviceId, UUID employeeUuid, PublicKey key, Instant now) {
    return new Device(deviceId, employeeUuid, key, DeviceStatus.ACTIVE, now, now);
  }

  public Device revoke(Instant now) {
    return status == DeviceStatus.REVOKED
        ? this
        : new Device(deviceId, employeeUuid, publicKey, DeviceStatus.REVOKED, registeredAt, now);
  }

  public boolean isActive() {
    return status == DeviceStatus.ACTIVE;
  }
}
