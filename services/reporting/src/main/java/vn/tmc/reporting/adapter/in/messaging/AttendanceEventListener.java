package vn.tmc.reporting.adapter.in.messaging;

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
 * Own subscription to every attendance event (document.md 12.5: one queue per consumer). Exchange
 * {@code tmc.events}, routing keys {@code <source>.<EventType>} (ADR-15). A message that still
 * fails after the listener retries goes to {@value #DLQ} instead of looping. Off on the inmem and
 * local profiles (no broker). Verified against RabbitMQ by AttendanceEventListenerIT. TODO Team D:
 * alerting on the DLQ.
 */
@Configuration
@Profile("!inmem & !local")
class AttendanceQueueConfig {
  static final String QUEUE = "tmc.reporting.attendance";
  static final String DLQ = QUEUE + ".dlq";

  @Bean
  TopicExchange tmcEvents() {
    return new TopicExchange(TmcExchange.NAME, true, false);
  }

  @Bean
  Queue reportingQueue() {
    return QueueBuilder.durable(QUEUE).deadLetterExchange("").deadLetterRoutingKey(DLQ).build();
  }

  @Bean
  Queue reportingDeadLetterQueue() {
    return QueueBuilder.durable(DLQ).build();
  }

  @Bean
  Binding reportingBinding(Queue reportingQueue, TopicExchange tmcEvents) {
    return BindingBuilder.bind(reportingQueue)
        .to(tmcEvents)
        .with(TmcExchange.everyEventOf(TmcExchange.ATTENDANCE));
  }

  @Bean
  MessageConverter amqpMessageConverter(ObjectMapper mapper) {
    return new Jackson2JsonMessageConverter(mapper);
  }
}

@Component
@Profile("!inmem & !local")
class AttendanceEventListener {
  private final IdempotentConsumer consumer;

  AttendanceEventListener(IdempotentConsumer reportingConsumer) {
    this.consumer = reportingConsumer;
  }

  @RabbitListener(queues = AttendanceQueueConfig.QUEUE)
  void onMessage(EventEnvelope envelope) {
    consumer.accept(envelope);
  }
}
