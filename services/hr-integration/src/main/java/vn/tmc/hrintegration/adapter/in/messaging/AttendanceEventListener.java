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

/**
 * Own subscription for HR Integration. Only AttendanceConfirmed and AttendanceCorrected are payroll
 * data. NOT exercised by automated tests (needs RabbitMQ). TODO Team D: DLQ, retry, JDBC dedupe
 * store.
 */
@Configuration
@Profile("!inmem")
class HrQueueConfig {
  static final String QUEUE = "tmc.hr.attendance";

  @Bean
  TopicExchange attendanceExchange() {
    return new TopicExchange("tmc.attendance.events", true, false);
  }

  @Bean
  Queue hrQueue() {
    return QueueBuilder.durable(QUEUE).build();
  }

  @Bean
  Binding hrConfirmedBinding(Queue hrQueue, TopicExchange attendanceExchange) {
    return BindingBuilder.bind(hrQueue)
        .to(attendanceExchange)
        .with("attendance.AttendanceConfirmed");
  }

  @Bean
  Binding hrCorrectedBinding(Queue hrQueue, TopicExchange attendanceExchange) {
    return BindingBuilder.bind(hrQueue)
        .to(attendanceExchange)
        .with("attendance.AttendanceCorrected");
  }

  @Bean
  MessageConverter amqpMessageConverter(ObjectMapper mapper) {
    return new Jackson2JsonMessageConverter(mapper);
  }
}

@Component
@Profile("!inmem")
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
