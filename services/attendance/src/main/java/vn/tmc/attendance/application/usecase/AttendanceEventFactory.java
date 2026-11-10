package vn.tmc.attendance.application.usecase;

import java.util.HashMap;
import java.util.Map;
import vn.tmc.attendance.application.port.in.GateResult;
import vn.tmc.attendance.domain.AccessAttempt;
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
