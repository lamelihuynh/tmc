package vn.tmc.platform.messaging;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProcessedMessageStore implements ProcessedMessageStore {
  private final Set<String> seen = ConcurrentHashMap.newKeySet();

  @Override
  public boolean markProcessed(String consumerName, UUID messageId) {
    return seen.add(consumerName + ":" + messageId);
  }
}
