package vn.tmc.platform.messaging;

import java.util.function.Consumer;
import vn.tmc.platform.kernel.EventEnvelope;

/**
 * Wraps a handler so redelivery of the same messageId is a no-op (DP-02). Services must run the
 * handler and the dedup marker in the same local transaction; this class only defines the contract
 * and the simple non-transactional flow used in tests and skeletons.
 */
public final class IdempotentConsumer {
  private final String consumerName;
  private final ProcessedMessageStore store;
  private final Consumer<EventEnvelope> handler;

  public IdempotentConsumer(
      String consumerName, ProcessedMessageStore store, Consumer<EventEnvelope> handler) {
    this.consumerName = consumerName;
    this.store = store;
    this.handler = handler;
  }

  /**
   * @return true if the handler ran, false if the message was a duplicate.
   */
  public boolean accept(EventEnvelope envelope) {
    if (!store.markProcessed(consumerName, envelope.messageId())) {
      return false;
    }
    handler.accept(envelope);
    return true;
  }
}
