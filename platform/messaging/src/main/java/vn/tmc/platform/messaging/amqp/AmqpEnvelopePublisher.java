package vn.tmc.platform.messaging.amqp;

import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.messaging.EventPublisher;
import vn.tmc.platform.messaging.TmcExchange;

/**
 * Publishes to {@link TmcExchange#NAME} with routing key {@code <source>.<eventType>} and waits for
 * the publisher confirm (ADR-04, ADR-15). No confirm in time or a nack count as "not published", so
 * the outbox row stays PENDING. A message no queue is bound for is confirmed and dropped by the
 * broker, on purpose: a consumer that is not deployed yet must not stall the producer's outbox.
 *
 * <p>The RabbitTemplate needs {@code spring.rabbitmq.publisher-confirm-type=correlated} and a JSON
 * message converter; the service declares the exchange ({@code new TopicExchange(TmcExchange.NAME,
 * true, false)}).
 */
public class AmqpEnvelopePublisher implements EventPublisher {
  private final RabbitTemplate rabbit;
  private final String source;
  private final long confirmTimeoutMs;

  /**
   * @param source routing prefix: {@link TmcExchange#ATTENDANCE}, {@link TmcExchange#USER_QR}...
   */
  public AmqpEnvelopePublisher(RabbitTemplate rabbit, String source, long confirmTimeoutMs) {
    this.rabbit = rabbit;
    this.source = source;
    this.confirmTimeoutMs = confirmTimeoutMs;
  }

  public String routingKey(EventEnvelope envelope) {
    return TmcExchange.routingKey(source, envelope.eventType());
  }

  @Override
  public void publish(EventEnvelope envelope) throws PublishFailedException {
    var correlation = new CorrelationData(envelope.messageId().toString());
    try {
      rabbit.convertAndSend(
          TmcExchange.NAME,
          routingKey(envelope),
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
