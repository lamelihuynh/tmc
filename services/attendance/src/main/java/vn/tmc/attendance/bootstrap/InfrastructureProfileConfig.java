package vn.tmc.attendance.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import vn.tmc.attendance.adapter.out.http.HttpGateStatusQuery;
import vn.tmc.attendance.adapter.out.messaging.AmqpEventPublisher;
import vn.tmc.attendance.adapter.out.persistence.JdbcAttemptRepository;
import vn.tmc.attendance.adapter.out.persistence.JdbcAttendanceTransactionRepository;
import vn.tmc.attendance.adapter.out.persistence.JdbcOutboxRepository;
import vn.tmc.attendance.adapter.out.persistence.SpringTransactionRunner;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.application.port.out.IGateStatusQuery;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.application.port.out.ITransactionRunner;
import vn.tmc.platform.messaging.EventPublisher;

/**
 * Default profile: PostgreSQL (Flyway), RabbitMQ with publisher confirms, HTTP query to Gate
 * Station.
 */
@Configuration
@Profile("!inmem & !local")
public class InfrastructureProfileConfig {

  @Bean
  IAttemptRepository attempts(JdbcTemplate jdbc) {
    return new JdbcAttemptRepository(jdbc);
  }

  @Bean
  IAttendanceTransactionRepository transactions(JdbcTemplate jdbc) {
    return new JdbcAttendanceTransactionRepository(jdbc);
  }

  @Bean
  IOutboxRepository outbox(JdbcTemplate jdbc, ObjectMapper mapper) {
    return new JdbcOutboxRepository(jdbc, mapper);
  }

  @Bean
  ITransactionRunner transactionRunner(PlatformTransactionManager manager) {
    return new SpringTransactionRunner(new TransactionTemplate(manager));
  }

  @Bean
  TopicExchange attendanceExchange() {
    return new TopicExchange(AmqpEventPublisher.EXCHANGE, true, false);
  }

  @Bean
  MessageConverter amqpMessageConverter(ObjectMapper mapper) {
    return new Jackson2JsonMessageConverter(mapper);
  }

  @Bean
  EventPublisher eventPublisher(
      RabbitTemplate rabbit,
      @Value("${tmc.attendance.broker.confirm-timeout-ms:2000}") long timeout) {
    return new AmqpEventPublisher(rabbit, timeout);
  }

  @Bean
  IGateStatusQuery gateStatusQuery(@Value("${tmc.attendance.gate-station-url}") String url) {
    return new HttpGateStatusQuery(RestClient.builder().baseUrl(url).build());
  }
}
