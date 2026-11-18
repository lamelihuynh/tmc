package vn.tmc.hrintegration.adapter.in.messaging;

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
 * Own subscription for HR Integration. Only AttendanceConfirmed and AttendanceCorrected are payroll
 * data. Exchange {@code tmc.events}, routing keys {@code <source>.<EventType>} (ADR-15). A message
 * that still fails after the listener retries goes to {@value #DLQ} instead of looping. Off on the
 * inmem and local profiles (no broker). Verified against RabbitMQ by HrAttendanceListenerIT. TODO
 * Team D: alerting on the DLQ.
 */
@Configuration
@Profile("!inmem & !local")
class HrQueueConfig {
  static final String QUEUE = "tmc.hr.attendance";
  static final String DLQ = QUEUE + ".dlq";

  @Bean
  TopicExchange tmcEvents() {
    return new TopicExchange(TmcExchange.NAME, true, false);
  }

  @Bean
  Queue hrintegrationQueue() {
    return QueueBuilder.durable(QUEUE).deadLetterExchange("").deadLetterRoutingKey(DLQ).build();
  }

  @Bean
  Queue hrintegrationDeadLetterQueue() {
    return QueueBuilder.durable(DLQ).build();
  }

  @Bean
  Binding hrConfirmedBinding(Queue hrintegrationQueue, TopicExchange tmcEvents) {
    return BindingBuilder.bind(hrintegrationQueue)
        .to(tmcEvents)
        .with(TmcExchange.routingKey(TmcExchange.ATTENDANCE, "AttendanceConfirmed"));
  }

  @Bean
  Binding hrCorrectedBinding(Queue hrintegrationQueue, TopicExchange tmcEvents) {
    return BindingBuilder.bind(hrintegrationQueue)
        .to(tmcEvents)
        .with(TmcExchange.routingKey(TmcExchange.ATTENDANCE, "AttendanceCorrected"));
  }

  @Bean
  MessageConverter amqpMessageConverter(ObjectMapper mapper) {
    return new Jackson2JsonMessageConverter(mapper);
  }
}

@Component
@Profile("!inmem & !local")
class HrAttendanceListener {
  private final IdempotentConsumer consumer;

  HrAttendanceListener(IdempotentConsumer hrConsumer) {
    this.consumer = hrConsumer;
  }

  @RabbitListener(queues = HrQueueConfig.QUEUE)
  void onMessage(EventEnvelope envelope) {
    consumer.accept(envelope);
  }
}
