package vn.tmc.platform.kernel;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker for facts that already happened inside an aggregate. Payload must stay PII-free (ADR-09,
 * DP-06).
 */
public interface DomainEvent {
  /** Stable business identifier (e.g. attempt_id). Same across retries. */
  UUID eventId();

  String eventType();

  Instant occurredAt();
}
