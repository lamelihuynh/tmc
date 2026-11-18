package vn.tmc.platform.messaging;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.Ids;

/**
 * Builds AuditRecorded envelopes for user-qr, identity and gate-station (C-08, AsyncAPI channels
 * {@code <source>.AuditRecorded}). Payload: {@code action} (required), {@code target}, {@code
 * result}, optional {@code actor} (internal UUID). Never a name, token or free text with personal
 * data (ADR-09).
 */
public final class AuditEvents {
  private AuditEvents() {}

  public static final String EVENT_TYPE = "AuditRecorded";

  /**
   * @param source short service name ({@link TmcExchange#USER_QR}...), also the routing prefix
   * @param subjectId what the entry is about (deviceId as UUID, account subject...): the eventId
   */
  public static EventEnvelope recorded(
      String source,
      UUID subjectId,
      String action,
      String target,
      String result,
      UUID actor,
      String correlationId,
      Instant at) {
    if (action == null || action.isBlank()) {
      throw new IllegalArgumentException("action is required");
    }
    Map<String, Object> payload = new HashMap<>();
    payload.put("action", action);
    if (target != null) {
      payload.put("target", target);
    }
    if (result != null) {
      payload.put("result", result);
    }
    if (actor != null) {
      payload.put("actor", actor.toString());
    }
    return new EventEnvelope(
        Ids.uuidV7(),
        EVENT_TYPE,
        1,
        subjectId == null ? Ids.uuidV7() : subjectId,
        1,
        at,
        correlationId,
        source,
        null,
        null,
        payload);
  }
}
