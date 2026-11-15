package vn.tmc.platform.messaging;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import vn.tmc.platform.kernel.EventEnvelope;

/**
 * Wraps a handler so redelivery of the same messageId is a no-op (DP-02). Services must run the
 * handler and the dedup marker in the same local transaction; this class only defines the contract
 * and the simple non-transactional flow used in tests and skeletons.
 */
public final class IdempotentConsumer {
  /** Runs the marker and the handler as one unit, e.g. a Spring TransactionTemplate. */
  @FunctionalInterface
  public interface TransactionRunner {
    boolean inTransaction(BooleanSupplier work);

    static TransactionRunner none() {
      return BooleanSupplier::getAsBoolean;
    }
  }

  private final String consumerName;
  private final ProcessedMessageStore store;
  private final Consumer<EventEnvelope> handler;
  private final TransactionRunner tx;

  public IdempotentConsumer(
      String consumerName, ProcessedMessageStore store, Consumer<EventEnvelope> handler) {
    this(consumerName, store, handler, TransactionRunner.none());
  }

  /**
   * With a database-backed store, pass a runner around the same DataSource so a handler failure
   * rolls the marker back and the redelivered message is processed again.
   */
  public IdempotentConsumer(
      String consumerName,
      ProcessedMessageStore store,
      Consumer<EventEnvelope> handler,
      TransactionRunner tx) {
    this.consumerName = consumerName;
    this.store = store;
    this.handler = handler;
    this.tx = tx;
  }

  public String consumerName() {
    return consumerName;
  }

  /**
   * @return true if the handler ran, false if the message was a duplicate.
   */
  public boolean accept(EventEnvelope envelope) {
    return tx.inTransaction(
        () -> {
          if (!store.markProcessed(consumerName, envelope.messageId())) {
            return false;
          }
          handler.accept(envelope);
          return true;
        });
  }
}
