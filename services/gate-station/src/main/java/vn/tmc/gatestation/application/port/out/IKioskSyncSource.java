package vn.tmc.gatestation.application.port.out;

import java.time.Instant;
import java.util.List;
import vn.tmc.gatestation.application.port.in.IKioskSupport.DeviceKey;

/**
 * Driven port: where the device keys of the kiosk sync bundle come from (User/QR, GET
 * /internal/v1/sync-bundle). Kiosks need them to verify tokens while offline.
 */
public interface IKioskSyncSource {
  /** Keys registered or revoked after {@code since}. */
  List<DeviceKey> deviceKeysChangedSince(Instant since);
}
