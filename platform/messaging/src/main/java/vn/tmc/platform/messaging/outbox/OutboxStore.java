package vn.tmc.platform.messaging.outbox;

import java.util.List;
import java.util.UUID;
import vn.tmc.platform.kernel.EventEnvelope;

/**
 * Transactional outbox (ADR-04) for any service. {@link #add} must run in the same local
 * transaction as the state change it describes; {@link OutboxRelay} publishes later. Table: see
 * {@link vn.tmc.platform.messaging.jdbc.JdbcOutboxStore}.
 */
public interface OutboxStore {
  void add(EventEnvelope envelope);

  /** PENDING rows in insertion order, which is the publish order. */
  List<EventEnvelope> fetchPending(int limit);

  /** Call only after the broker confirmed the message. */
  void markPublished(UUID messageId);

  /** The row stays PENDING; the counter feeds the backlog metric. */
  void recordFailure(UUID messageId);
}
