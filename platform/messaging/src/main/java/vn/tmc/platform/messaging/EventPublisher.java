package vn.tmc.platform.messaging;

import vn.tmc.platform.kernel.EventEnvelope;

/** Outbound messaging port. Implemented by an AMQP adapter or an in-memory broker for tests. */
public interface EventPublisher {

  /**
   * Publishes and returns only after the broker acknowledged (publisher confirm). Throwing means
   * "not confirmed": the caller must keep the outbox row PENDING and retry (ADR-04).
   */
  void publish(EventEnvelope envelope) throws PublishFailedException;

  class PublishFailedException extends Exception {
    public PublishFailedException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
