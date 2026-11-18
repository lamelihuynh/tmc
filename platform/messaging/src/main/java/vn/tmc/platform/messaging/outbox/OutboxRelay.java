package vn.tmc.platform.messaging.outbox;

import vn.tmc.platform.messaging.EventPublisher;

/**
 * Moves outbox rows to the broker (ADR-04): PUBLISHED only after the broker confirmed. Stops at the
 * first failure to keep order; the rest stays PENDING for the next run. A lost confirm can cause a
 * resend, which consumers absorb by deduplicating on messageId. Call {@link #runOnce} from a
 * scheduler in the service's bootstrap.
 */
public class OutboxRelay {
  private final OutboxStore outbox;
  private final EventPublisher publisher;
  private final int batchSize;

  public OutboxRelay(OutboxStore outbox, EventPublisher publisher, int batchSize) {
    this.outbox = outbox;
    this.publisher = publisher;
    this.batchSize = batchSize;
  }

  public record Report(int published, int stillPending) {}

  public Report runOnce() {
    var pending = outbox.fetchPending(batchSize);
    int published = 0;
    for (var envelope : pending) {
      try {
        publisher.publish(envelope);
        outbox.markPublished(envelope.messageId());
        published++;
      } catch (EventPublisher.PublishFailedException e) {
        outbox.recordFailure(envelope.messageId());
        break;
      }
    }
    return new Report(published, pending.size() - published);
  }
}
