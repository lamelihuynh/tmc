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

/**
 * Own subscription to the AttendanceEvents exchange (document.md 12.5: one subscription per
 * consumer). NOT exercised by automated tests (needs RabbitMQ). TODO Team D: dead-letter queue,
 * retry policy, and a database-backed ProcessedMessageStore updated in the same transaction as the
 * read model.
 */
@Configuration
@Profile("!inmem")
class AttendanceQueueConfig {
  static final String EXCHANGE = "tmc.attendance.events";
  static final String QUEUE = "tmc.reporting.attendance";

  @Bean
  TopicExchange attendanceExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  Queue reportingQueue() {
    return QueueBuilder.durable(QUEUE).build();
  }

  @Bean
  Binding reportingBinding(Queue reportingQueue, TopicExchange attendanceExchange) {
    return BindingBuilder.bind(reportingQueue).to(attendanceExchange).with("attendance.*");
  }

  @Bean
  MessageConverter amqpMessageConverter(ObjectMapper mapper) {
    return new Jackson2JsonMessageConverter(mapper);
  }
}

@Component
@Profile("!inmem")
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
