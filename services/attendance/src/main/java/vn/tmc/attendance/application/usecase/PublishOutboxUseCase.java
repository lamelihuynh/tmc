package vn.tmc.attendance.application.usecase;

import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.platform.messaging.EventPublisher;

/**
 * Outbox publisher (ADR-04). A row becomes PUBLISHED only after the broker confirmed it. On the
 * first failure it stops and keeps order; the rows stay PENDING and are retried on the next run.
 * Consumers deduplicate by messageId because a lost confirm can cause a resend.
 */
public class PublishOutboxUseCase {
  private final IOutboxRepository outbox;
  private final EventPublisher publisher;
  private final int batchSize;

  public PublishOutboxUseCase(IOutboxRepository outbox, EventPublisher publisher, int batchSize) {
    this.outbox = outbox;
    this.publisher = publisher;
    this.batchSize = batchSize;
  }

  public record Report(int published, int stillPending) {}

  public Report run() {
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
