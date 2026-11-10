package vn.tmc.platform.messaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vn.tmc.platform.kernel.EventEnvelope;

/** Test double with a switch to simulate "broker unavailable" (ADR-11 test 4). */
public class InMemoryEventBroker implements EventPublisher {
  private final List<EventEnvelope> published = Collections.synchronizedList(new ArrayList<>());
  private volatile boolean available = true;

  public void setAvailable(boolean available) {
    this.available = available;
  }

  @Override
  public void publish(EventEnvelope envelope) throws PublishFailedException {
    if (!available) {
      throw new PublishFailedException("broker unavailable", null);
    }
    published.add(envelope);
  }

  public List<EventEnvelope> published() {
    synchronized (published) {
      return List.copyOf(published);
    }
  }
}
