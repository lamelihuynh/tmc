package vn.tmc.kiosksim;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The kiosk's view of the Edge Gateway API (document.md table 16.2). */
public interface EdgeClient {

  class EdgeUnreachableException extends Exception {
    public EdgeUnreachableException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  record ScanResponse(
      UUID attemptId, String outcome, String reasonCode, String direction, boolean duplicate) {}

  record OfflineLine(
      UUID attemptId,
      long journalSeq,
      String employeeUuid,
      String gateId,
      String direction,
      String jti,
      Instant scannedAt,
      String localStatus,
      String deniedReason) {}

  record OfflineResult(UUID attemptId, String outcome, String detail) {}

  record Employee(UUID employeeUuid, String status, List<String> factories) {}

  /**
   * @param employees null if the edge does not send the directory (older edge)
   * @param generatedAt the edge's time of the bundle, the cursor of the next sync; null if absent
   */
  record SyncBundle(
      List<Journal.Gate> gates,
      List<Journal.DeviceKey> deviceKeys,
      List<Employee> employees,
      Instant generatedAt) {

    public SyncBundle(
        List<Journal.Gate> gates, List<Journal.DeviceKey> deviceKeys, List<Employee> employees) {
      this(gates, deviceKeys, employees, null);
    }

    public SyncBundle(List<Journal.Gate> gates, List<Journal.DeviceKey> deviceKeys) {
      this(gates, deviceKeys, null, null);
    }
  }

  /**
   * @param tokenOrHash the raw token for a first attempt, the stored hash for a retry of an
   *     unfinished one
   */
  ScanResponse scan(
      UUID attemptId,
      String gateId,
      String tokenOrHash,
      Instant scannedAt,
      long journalSeq,
      String kioskId)
      throws EdgeUnreachableException;

  SyncBundle sync(Instant since) throws EdgeUnreachableException;

  List<OfflineResult> offlineBatch(String kioskId, List<OfflineLine> lines)
      throws EdgeUnreachableException;
}
