package vn.tmc.attendance.application.usecase;

import java.util.HashMap;
import java.util.Map;
import vn.tmc.attendance.application.port.in.GateResult;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttendanceCorrection;
import vn.tmc.attendance.domain.AttendanceTransaction;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.Ids;

/**
 * Builds the AttendanceEvents of document.md table 12.6.1. Payload is minimum data and PII-free:
 * internal employee UUID only, never a name, never the raw QR token or its jti (ADR-09, DP-06).
 */
public class AttendanceEventFactory {
  private static final String SOURCE = "attendance-edge";
  private final IClock clock;

  public AttendanceEventFactory(IClock clock) {
    this.clock = clock;
  }

  public EventEnvelope accessAttemptRecorded(AccessAttempt a, String correlationId) {
    return build("AccessAttemptRecorded", a, correlationId, basePayload(a));
  }

  public EventEnvelope gateResultRecorded(
      AccessAttempt a, GateResult result, String correlationId) {
    Map<String, Object> payload = basePayload(a);
    payload.put("gateResult", result.name());
    return build("GateResultRecorded", a, correlationId, payload);
  }

  public EventEnvelope attendanceConfirmed(AccessAttempt a, String correlationId) {
    return build("AttendanceConfirmed", a, correlationId, basePayload(a));
  }

  public EventEnvelope auditRecorded(AccessAttempt a, String action, String correlationId) {
    Map<String, Object> payload = basePayload(a);
    payload.put("action", action);
    return build("AuditRecorded", a, correlationId, payload);
  }

  /**
   * AttendanceCorrected (AsyncAPI channel attendance.AttendanceCorrected). {@code eventId} is the
   * original attempt id so consumers find the row to replace. {@code aggregateVersion} is the
   * version the original attempt had when it was confirmed plus the revision, so it is larger than
   * every earlier event of the same eventId and consumers keep the highest one.
   *
   * @param originalVersion {@link AccessAttempt#version()} of the original attempt (it no longer
   *     changes once COMMITTED)
   */
  public EventEnvelope attendanceCorrected(
      AttendanceTransaction original,
      long originalVersion,
      AttendanceCorrection correction,
      String correlationId) {
    if (!original.attemptId().equals(correction.originalAttemptId())) {
      throw new IllegalArgumentException("correction does not belong to this transaction");
    }
    Map<String, Object> payload = new HashMap<>();
    payload.put("status", "CORRECTED");
    payload.put("correctionId", correction.correctionId().toString());
    payload.put("originalAttemptId", correction.originalAttemptId().toString());
    payload.put("revision", correction.revision());
    payload.put("employeeUuid", correction.employeeUuid().toString());
    payload.put("direction", correction.direction().name());
    payload.put("scannedAt", correction.scannedAt().toString());
    payload.put("previousDirection", correction.previousDirection().name());
    payload.put("previousScannedAt", correction.previousScannedAt().toString());
    payload.put("reason", correction.reason());
    payload.put("correctedBy", correction.correctedBy().toString());
    payload.put("source", correction.source().name());
    return new EventEnvelope(
        Ids.uuidV7(),
        "AttendanceCorrected",
        1,
        original.attemptId(),
        correctedVersion(originalVersion, correction.revision()),
        clock.now(),
        correlationId,
        SOURCE,
        original.factoryCode(),
        original.gateId(),
        payload);
  }

  /** The aggregateVersion rule of AttendanceCorrected, exposed so consumers' tests can share it. */
  public static long correctedVersion(long originalVersion, int revision) {
    return originalVersion + revision;
  }

  private Map<String, Object> basePayload(AccessAttempt a) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("status", a.status().name());
    payload.put("source", a.source().name());
    payload.put("scannedAt", a.scannedAt().toString());
    if (a.employeeUuid() != null) {
      payload.put("employeeUuid", a.employeeUuid().toString());
    }
    if (a.direction() != null) {
      payload.put("direction", a.direction().name());
    }
    if (a.reasonCode() != null) {
      payload.put("reasonCode", a.reasonCode().name());
    }
    if (a.anomaly() != null) {
      payload.put("anomaly", a.anomaly().name());
    }
    return payload;
  }

  private EventEnvelope build(
      String type, AccessAttempt a, String correlationId, Map<String, Object> payload) {
    return new EventEnvelope(
        Ids.uuidV7(),
        type,
        1,
        a.attemptId(),
        a.version(),
        clock.now(),
        correlationId,
        SOURCE,
        a.factoryCode(),
        a.gateId(),
        payload);
  }
}
