package vn.tmc.platform.kernel;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Message contract for AttendanceEvents (document.md section 12.6). eventId identifies the business
 * transaction, messageId identifies one concrete message (retries keep it). Payload carries minimum
 * data only; never raw QR tokens or readable PII (ADR-09).
 */
public record EventEnvelope(
    UUID messageId,
    String eventType,
    int schemaVersion,
    UUID eventId,
    long aggregateVersion,
    Instant occurredAt,
    String correlationId,
    String source,
    String factoryCode,
    String gateId,
    Map<String, Object> payload) {

  public EventEnvelope {
    payload = payload == null ? Map.of() : Map.copyOf(payload);
  }
}
