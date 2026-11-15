package vn.tmc.gatestation.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.domain.GateConfig;
import vn.tmc.platform.kernel.ReasonCode;

/** Sync bundle and heartbeat endpoints used by kiosks (document.md 16.2 rows 3 and 4). */
public interface IKioskSupport {

  record DeviceKey(
      String deviceId, String employeeUuid, String publicKeyX509Base64, boolean revoked) {}

  /** docs/api/kiosk-edge.openapi.yaml: status is ACTIVE or INACTIVE. */
  record EmployeeEligibility(String employeeUuid, String status, List<String> factories) {}

  record SyncBundle(
      Instant generatedAt,
      List<GateConfig> gates,
      List<DeviceKey> deviceKeys,
      List<EmployeeEligibility> employees) {
    public SyncBundle(Instant generatedAt, List<GateConfig> gates, List<DeviceKey> deviceKeys) {
      this(generatedAt, gates, deviceKeys, List.of());
    }
  }

  record Heartbeat(
      String kioskId,
      String gateId,
      String readerStatus,
      int offlineQueueSize,
      String softwareVersion) {}

  enum OfflineOutcome {
    ACCEPTED,
    DUPLICATE,
    ANOMALY,
    REJECTED
  }

  /**
   * One journal entry from a kiosk. The factory is NOT taken from the kiosk, it comes from the gate
   * catalog.
   */
  record OfflineEntry(
      UUID attemptId,
      long journalSeq,
      UUID employeeUuid,
      String gateId,
      Direction direction,
      String jti,
      Instant scannedAt,
      String localStatus,
      ReasonCode deniedReason) {}

  record OfflineEntryResult(UUID attemptId, OfflineOutcome outcome, String detail) {}

  SyncBundle syncBundle(Instant since);

  List<OfflineEntryResult> ingestOffline(String kioskId, List<OfflineEntry> entries);

  void heartbeat(Heartbeat heartbeat);
}
