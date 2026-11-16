package vn.tmc.auditadmin.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.TmcExchange;

/**
 * Own subscription for AuditRecorded from every source (attendance, user-qr, identity,
 * gate-station). Exchange {@code tmc.events}, routing keys {@code <source>.<EventType>} (ADR-15). A
 * message that still fails after the listener retries goes to {@value #DLQ} instead of looping. Off
 * on the inmem and local profiles (no broker). Verified against RabbitMQ by AuditEventListenerIT.
 * TODO Team D: alerting on the DLQ.
 */
@Configuration
@Profile("!inmem & !local")
class AuditQueueConfig {
  static final String QUEUE = "tmc.audit.events";
  static final String DLQ = QUEUE + ".dlq";

  @Bean
  TopicExchange tmcEvents() {
    return new TopicExchange(TmcExchange.NAME, true, false);
  }

  @Bean
  Queue auditadminQueue() {
    return QueueBuilder.durable(QUEUE).deadLetterExchange("").deadLetterRoutingKey(DLQ).build();
  }

  @Bean
  Queue auditadminDeadLetterQueue() {
    return QueueBuilder.durable(DLQ).build();
  }

  @Bean
  Binding auditBinding(Queue auditadminQueue, TopicExchange tmcEvents) {
    return BindingBuilder.bind(auditadminQueue)
        .to(tmcEvents)
        .with(TmcExchange.anySource("AuditRecorded"));
  }

  @Bean
  MessageConverter amqpMessageConverter(ObjectMapper mapper) {
    return new Jackson2JsonMessageConverter(mapper);
  }
}

@Component
@Profile("!inmem & !local")
class AuditEventListener {
  private final IdempotentConsumer consumer;

  AuditEventListener(IdempotentConsumer auditConsumer) {
    this.consumer = auditConsumer;
  }

  @RabbitListener(queues = AuditQueueConfig.QUEUE)
  void onMessage(EventEnvelope envelope) {
    consumer.accept(envelope);
  }
}
