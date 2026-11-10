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

/** Own subscription for audit events. NOT exercised by automated tests (needs RabbitMQ). */
@Configuration
@Profile("!inmem")
class AuditQueueConfig {
  static final String QUEUE = "tmc.audit.events";

  @Bean
  TopicExchange attendanceExchange() {
    return new TopicExchange("tmc.attendance.events", true, false);
  }

  @Bean
  Queue auditQueue() {
    return QueueBuilder.durable(QUEUE).build();
  }

  @Bean
  Binding auditBinding(Queue auditQueue, TopicExchange attendanceExchange) {
    return BindingBuilder.bind(auditQueue).to(attendanceExchange).with("attendance.AuditRecorded");
  }

  @Bean
  MessageConverter amqpMessageConverter(ObjectMapper mapper) {
    return new Jackson2JsonMessageConverter(mapper);
  }
}

@Component
@Profile("!inmem")
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
