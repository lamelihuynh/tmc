package vn.tmc.attendance.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.platform.kernel.EventEnvelope;

/**
 * PostgreSQL outbox over outbox_events. The whole envelope is stored as JSONB; seq (BIGSERIAL)
 * gives the publish order. Rows are inserted through the same JdbcTemplate, and therefore the same
 * transaction, as the attempt they describe.
 *
 * <p>NOT yet run against a real database. TODO Team B (B-01).
 */
public class JdbcOutboxRepository implements IOutboxRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;

  public JdbcOutboxRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  @Override
  public void add(EventEnvelope envelope) {
    jdbc.update(
        "INSERT INTO outbox_events (message_id, event_type, envelope) VALUES (?, ?, ?::jsonb)",
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
