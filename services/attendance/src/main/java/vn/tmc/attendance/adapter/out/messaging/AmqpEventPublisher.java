package vn.tmc.attendance.adapter.out.messaging;

import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.messaging.EventPublisher;

/**
 * Publishes to the topic exchange {@value #EXCHANGE} with routing key {@code
 * attendance.<eventType>} and waits for the publisher confirm (ADR-04). No confirm in time, a nack
 * or any exception is reported as "not published", so the outbox row stays PENDING.
 *
 * <p>Needs {@code spring.rabbitmq.publisher-confirm-type=correlated} (set in application.yml). NOT
 * yet run against a real broker. TODO Team B (B-02): Testcontainers test that stops and restarts
 * RabbitMQ.
 */
public class AmqpEventPublisher implements EventPublisher {
  public static final String EXCHANGE = "tmc.attendance.events";
  public static final String ROUTING_PREFIX = "attendance.";

  private final RabbitTemplate rabbit;
  private final long confirmTimeoutMs;

  public AmqpEventPublisher(RabbitTemplate rabbit, long confirmTimeoutMs) {
    this.rabbit = rabbit;
    this.confirmTimeoutMs = confirmTimeoutMs;
  }

  @Override
  public void publish(EventEnvelope envelope) throws PublishFailedException {
    var correlation = new CorrelationData(envelope.messageId().toString());
    try {
      rabbit.convertAndSend(
          EXCHANGE,
          ROUTING_PREFIX + envelope.eventType(),
          envelope,
          message -> {
            message.getMessageProperties().setMessageId(envelope.messageId().toString());
            return message;
          },
          correlation);
      var confirm = correlation.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
      if (confirm == null || !confirm.isAck()) {
        throw new PublishFailedException("broker rejected message " + envelope.messageId(), null);
      }
    } catch (PublishFailedException e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new PublishFailedException("interrupted while waiting for the confirm", e);
    } catch (Exception e) {
      throw new PublishFailedException("no confirm for message " + envelope.messageId(), e);
    }
  }
}
