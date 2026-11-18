package vn.tmc.platform.messaging.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.messaging.outbox.OutboxStore;

/**
 * PostgreSQL outbox, same table shape as attendance's {@code outbox_events} (V1 there). The owning
 * service creates the table in its own Flyway migration; the DDL to copy is {@link #DDL} (also in
 * platform/messaging/src/main/resources/tmc-messaging/outbox_events.sql). Rows go through the same
 * JdbcTemplate, and therefore the same transaction, as the state change they describe.
 */
public class JdbcOutboxStore implements OutboxStore {
  public static final String DDL =
      """
      CREATE TABLE IF NOT EXISTS outbox_events (
          seq            BIGSERIAL PRIMARY KEY,
          message_id     UUID        NOT NULL UNIQUE,
          event_type     VARCHAR(48) NOT NULL,
          envelope       JSONB       NOT NULL,
          publish_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
          failure_count  INT         NOT NULL DEFAULT 0,
          created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
          published_at   TIMESTAMPTZ
      );
      CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox_events (publish_status, seq);
      """;

  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;

  public JdbcOutboxStore(JdbcTemplate jdbc, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  @Override
  public void add(EventEnvelope envelope) {
    jdbc.update(
        "INSERT INTO outbox_events (message_id, event_type, envelope) VALUES (?, ?, ?::jsonb) "
            + "ON CONFLICT (message_id) DO NOTHING",
        envelope.messageId(),
        envelope.eventType(),
        toJson(envelope));
  }

  @Override
  public List<EventEnvelope> fetchPending(int limit) {
    return jdbc.query(
        "SELECT envelope::text AS envelope FROM outbox_events "
            + "WHERE publish_status = 'PENDING' ORDER BY seq LIMIT ?",
        (rs, n) -> fromJson(rs.getString("envelope")),
        limit);
  }

  @Override
  public void markPublished(UUID messageId) {
    jdbc.update(
        "UPDATE outbox_events SET publish_status = 'PUBLISHED', published_at = now() "
            + "WHERE message_id = ?",
        messageId);
  }

  @Override
  public void recordFailure(UUID messageId) {
    jdbc.update(
        "UPDATE outbox_events SET failure_count = failure_count + 1 WHERE message_id = ?",
        messageId);
  }

  private String toJson(EventEnvelope envelope) {
    try {
      return mapper.writeValueAsString(envelope);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("cannot serialize event " + envelope.messageId(), e);
    }
  }

  private EventEnvelope fromJson(String json) {
    try {
      return mapper.readValue(json, EventEnvelope.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("cannot read stored event", e);
    }
  }
}
