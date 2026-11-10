package vn.tmc.userqr.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Delta pulled by Edge Agent and kiosks so they can verify and revoke offline (document.md 16.2 row
 * 3).
 */
public interface ISyncBundle {

  record DeviceKey(
      String deviceId, UUID employeeUuid, String publicKeyX509Base64, boolean revoked) {}

  record Bundle(Instant generatedAt, List<DeviceKey> deviceKeys) {}

  Bundle changedSince(Instant since);
}
