package vn.tmc.userqr.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.messaging.EventPublisher;
import vn.tmc.platform.messaging.InMemoryEventBroker;
import vn.tmc.platform.messaging.TmcExchange;
import vn.tmc.platform.messaging.amqp.AmqpEnvelopePublisher;
import vn.tmc.platform.messaging.jdbc.JdbcOutboxStore;
import vn.tmc.platform.messaging.outbox.AuditOutbox;
import vn.tmc.platform.messaging.outbox.InMemoryOutboxStore;
import vn.tmc.platform.messaging.outbox.OutboxRelay;
import vn.tmc.platform.messaging.outbox.OutboxStore;

/**
 * Outbox and relay for this service's events (ADR-04, ADR-15): AuditRecorded on routing key {@code
 * user-qr.AuditRecorded} of {@code tmc.events}. Default profile: JdbcOutboxStore (table
 * outbox_events, Flyway V2) and RabbitMQ with publisher confirms. inmem and local profiles:
 * in-memory outbox and broker, no Docker. The {@link AuditOutbox} bean is ready for C-08: point the
 * IAuditLog bean at it.
 */
@Configuration
@EnableScheduling
class OutboxConfig {
  private static final Logger LOG = LoggerFactory.getLogger(OutboxConfig.class);

  @Bean
  AuditOutbox auditOutbox(OutboxStore outbox, IClock clock) {
    return new AuditOutbox(outbox, TmcExchange.USER_QR, clock);
  }

  @Bean
  OutboxRelay outboxRelay(
      OutboxStore outbox,
      EventPublisher publisher,
      @Value("${tmc.outbox.batch-size:100}") int batch) {
    return new OutboxRelay(outbox, publisher, batch);
  }

  @Configuration
  @Profile({"inmem", "local"})
  static class InMemory {
    @Bean
    OutboxStore outboxStore() {
      return new InMemoryOutboxStore();
    }

    @Bean
    EventPublisher eventPublisher() {
      return new InMemoryEventBroker();
    }
  }

  @Configuration
  @Profile("!inmem & !local")
  static class Broker {
    @Bean
    OutboxStore outboxStore(JdbcTemplate jdbc, ObjectMapper mapper) {
      return new JdbcOutboxStore(jdbc, mapper);
    }

    @Bean
    TopicExchange tmcEvents() {
      return new TopicExchange(TmcExchange.NAME, true, false);
    }

    @Bean
    MessageConverter amqpMessageConverter(ObjectMapper mapper) {
      return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    EventPublisher eventPublisher(
        RabbitTemplate rabbit, @Value("${tmc.outbox.confirm-timeout-ms:2000}") long timeout) {
      return new AmqpEnvelopePublisher(rabbit, TmcExchange.USER_QR, timeout);
    }
  }

  @Configuration
  @ConditionalOnProperty(
      name = "tmc.outbox.relay-enabled",
      havingValue = "true",
      matchIfMissing = true)
  static class RelayJob {
    private final OutboxRelay relay;

    RelayJob(OutboxRelay relay) {
      this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${tmc.outbox.relay-interval-ms:2000}")
    void relay() {
      var report = relay.runOnce();
      if (report.stillPending() > 0) {
        LOG.warn("outbox: {} events still pending (broker unavailable?)", report.stillPending());
      }
    }
  }
}
