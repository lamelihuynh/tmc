package vn.tmc.attendance.application.port.out;

import java.util.List;
import java.util.UUID;
import vn.tmc.platform.kernel.EventEnvelope;

/**
 * Driven port: the transactional outbox (table outbox_events, ADR-04). {@link #add} must run in the
 * same local transaction as the state change it describes.
 */
public interface IOutboxRepository {
  void add(EventEnvelope envelope);

  /** PENDING rows in insertion order, which is the publish order. */
  List<EventEnvelope> fetchPending(int limit);

  /** Call only after the broker confirmed the message. */
  void markPublished(UUID messageId);

  /** The row stays PENDING; the counter feeds the backlog metric. */
  void recordFailure(UUID messageId);
}
