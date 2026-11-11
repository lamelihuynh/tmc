package vn.tmc.attendance.adapter.out.inmem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.platform.kernel.EventEnvelope;

/** Process-memory outbox that keeps insertion order, like the BIGSERIAL seq of outbox_events. */
public class InMemoryOutboxRepository implements IOutboxRepository {
  private final List<EventEnvelope> rows = new ArrayList<>();
  private final Set<UUID> published = new HashSet<>();

  @Override
  public synchronized void add(EventEnvelope envelope) {
    rows.add(envelope);
  }

  @Override
  public synchronized List<EventEnvelope> fetchPending(int limit) {
    return rows.stream().filter(e -> !published.contains(e.messageId())).limit(limit).toList();
  }

  @Override
  public synchronized void markPublished(UUID messageId) {
    published.add(messageId);
  }

  @Override
  public synchronized void recordFailure(UUID messageId) {
    // nothing to count in memory; the row simply stays pending
  }

  /** For tests: every event ever written, published or not. */
  public synchronized List<EventEnvelope> all() {
    return List.copyOf(rows);
  }

  /** For tests. */
  public synchronized long pendingCount() {
    return rows.stream().filter(e -> !published.contains(e.messageId())).count();
  }
}
