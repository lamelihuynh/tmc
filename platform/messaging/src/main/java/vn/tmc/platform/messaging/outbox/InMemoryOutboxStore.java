package vn.tmc.platform.messaging.outbox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import vn.tmc.platform.kernel.EventEnvelope;

/**
 * Outbox in process memory: for tests, the Docker-free local profile and services without a
 * database yet (identity). Pending events are lost on restart, so it is NOT a durable outbox.
 */
public class InMemoryOutboxStore implements OutboxStore {
  private final Map<UUID, EventEnvelope> pending = new LinkedHashMap<>();
  private final List<EventEnvelope> published = new ArrayList<>();
  private final Map<UUID, Integer> failures = new LinkedHashMap<>();

  @Override
  public synchronized void add(EventEnvelope envelope) {
    pending.putIfAbsent(envelope.messageId(), envelope);
  }

  @Override
  public synchronized List<EventEnvelope> fetchPending(int limit) {
    return pending.values().stream().limit(limit).toList();
  }

  @Override
  public synchronized void markPublished(UUID messageId) {
    var e = pending.remove(messageId);
    if (e != null) {
      published.add(e);
    }
  }

  @Override
  public synchronized void recordFailure(UUID messageId) {
    failures.merge(messageId, 1, Integer::sum);
  }

  public synchronized List<EventEnvelope> published() {
    return List.copyOf(published);
  }

  public synchronized int failures(UUID messageId) {
    return failures.getOrDefault(messageId, 0);
  }
}
