package vn.tmc.platform.messaging;

import java.util.UUID;

/** Per-consumer dedup store, owned by the consuming service (ADR-05, document.md section 12.7). */
public interface ProcessedMessageStore {

  /** Atomically records (consumer, messageId). Returns false if it was already recorded. */
  boolean markProcessed(String consumerName, UUID messageId);
}
