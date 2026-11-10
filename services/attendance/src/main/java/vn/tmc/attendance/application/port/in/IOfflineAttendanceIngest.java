package vn.tmc.attendance.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Ingests the kiosk journal after connectivity returns (ADR-11 section 5). It never sends an open
 * command: the gate already opened when the record was written.
 */
public interface IOfflineAttendanceIngest {

  List<OfflineResult> ingest(String kioskId, List<OfflineRecord> records);

  enum LocalStatus {
    OFFLINE_OPENED,
    OFFLINE_DENIED
  }

  enum Outcome {
    ACCEPTED,
    DUPLICATE,
    ANOMALY
  }

  record OfflineRecord(
      UUID attemptId,
      long journalSeq,
      UUID employeeUuid,
      String gateId,
      String factoryCode,
      Direction direction,
      String jti,
      Instant scannedAt,
      LocalStatus localStatus,
      ReasonCode deniedReason) {}

  record OfflineResult(UUID attemptId, Outcome outcome, String detail) {}
}
