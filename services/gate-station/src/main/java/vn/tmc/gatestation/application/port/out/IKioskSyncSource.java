package vn.tmc.gatestation.application.port.out;

import java.time.Instant;
import java.util.List;
import vn.tmc.gatestation.application.port.in.IKioskSupport.DeviceKey;
import vn.tmc.gatestation.domain.EmployeeAccess;

/**
 * Driven port: the sync bundle of User/QR (GET /internal/v1/sync-bundle): device keys so kiosks can
 * verify tokens while offline, and employee eligibility for {@link IEmployeeDirectoryCache}.
 */
public interface IKioskSyncSource {

  /**
   * @param generatedAt User/QR's clock when it built the delta: the next {@code since}; null when
   *     unknown
   */
  record Delta(Instant generatedAt, List<DeviceKey> deviceKeys, List<EmployeeAccess> employees) {
    public Delta {
      deviceKeys = deviceKeys == null ? List.of() : List.copyOf(deviceKeys);
      employees = employees == null ? List.of() : List.copyOf(employees);
    }
  }

  /** Keys registered or revoked after {@code since}. */
  List<DeviceKey> deviceKeysChangedSince(Instant since);

  /** Everything that changed after {@code since}. Sources without employees ship none. */
  default Delta changedSince(Instant since) {
    return new Delta(null, deviceKeysChangedSince(since), List.of());
  }
}
