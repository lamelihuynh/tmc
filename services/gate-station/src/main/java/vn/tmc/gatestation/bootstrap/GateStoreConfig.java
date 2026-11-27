package vn.tmc.gatestation.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tmc.gatestation.adapter.out.persistence.AuditOutboxAdapter;
import vn.tmc.gatestation.adapter.out.persistence.JdbcGateConfigRepository;
import vn.tmc.gatestation.adapter.out.persistence.TransactionalGateManagement;
import vn.tmc.gatestation.application.port.in.IGateManagement;
import vn.tmc.gatestation.application.port.out.IAuditLog;
import vn.tmc.gatestation.application.port.out.IGateConfigRepository;
import vn.tmc.gatestation.application.usecase.AdminGateService;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.messaging.EventPublisher;
import vn.tmc.platform.messaging.TmcExchange;
import vn.tmc.platform.messaging.amqp.AmqpEnvelopePublisher;
import vn.tmc.platform.messaging.jdbc.JdbcOutboxStore;
import vn.tmc.platform.messaging.outbox.AuditOutbox;
import vn.tmc.platform.messaging.outbox.OutboxRelay;
import vn.tmc.platform.messaging.outbox.OutboxStore;

/**
 * Gate Store (ADR-14): gate-station's own PostgreSQL database, only with the "gate-store" profile.
 * Migrations live in classpath:db/gate-store and run at start-up. Gate changes and their audit rows
 * share a transaction; audit events are relayed to RabbitMQ (ADR-15).
 */
@Configuration
@Profile("gate-store")
@EnableScheduling
class GateStoreConfig {

  @Bean(destroyMethod = "close")
  DataSource gateStoreDataSource(
      @Value("${tmc.gate.store.url}") String url,
      @Value("${tmc.gate.store.username}") String user,
      @Value("${tmc.gate.store.password}") String password) {
    var ds = new HikariDataSource();
    ds.setJdbcUrl(url);
    ds.setUsername(user);
    ds.setPassword(password);
    ds.setPoolName("gate-store");
    ds.setMaximumPoolSize(5);
    return ds;
  }

  @Bean
  Flyway gateStoreFlyway(DataSource gateStoreDataSource) {
    var flyway =
        Flyway.configure()
            .dataSource(gateStoreDataSource)
            .locations("classpath:db/gate-store")
            .load();
    flyway.migrate();
    return flyway;
  }

  @Bean
  JdbcTemplate gateStoreJdbc(DataSource gateStoreDataSource, Flyway gateStoreFlyway) {
    return new JdbcTemplate(gateStoreDataSource);
  }

  @Bean
  PlatformTransactionManager gateStoreTransactions(DataSource gateStoreDataSource) {
    return new DataSourceTransactionManager(gateStoreDataSource);
  }

  @Bean
  IGateConfigRepository gateConfigRepository(JdbcTemplate gateStoreJdbc) {
    return new JdbcGateConfigRepository(gateStoreJdbc);
  }

  @Bean
  OutboxStore gateStoreOutboxStore(JdbcTemplate gateStoreJdbc, ObjectMapper mapper) {
    return new JdbcOutboxStore(gateStoreJdbc, mapper);
  }

  @Bean
  AuditOutbox gateStoreAuditOutbox(OutboxStore gateStoreOutboxStore, IClock clock) {
    return new AuditOutbox(gateStoreOutboxStore, TmcExchange.GATE_STATION, clock);
  }

  @Bean
  IAuditLog gateStoreAuditLog(AuditOutbox gateStoreAuditOutbox) {
    return new AuditOutboxAdapter(gateStoreAuditOutbox);
  }

  @Bean
  IGateManagement gateManagement(
      IGateConfigRepository gates,
      IAuditLog audit,
      PlatformTransactionManager gateStoreTransactions) {
    var useCase = new AdminGateService(gates, audit);
    return new TransactionalGateManagement(useCase, new TransactionTemplate(gateStoreTransactions));
  }

  @Bean
  OutboxRelay gateStoreOutboxRelay(
      OutboxStore gateStoreOutboxStore,
      EventPublisher gateStoreEventPublisher,
      @Value("${tmc.outbox.batch-size:100}") int batchSize) {
    return new OutboxRelay(gateStoreOutboxStore, gateStoreEventPublisher, batchSize);
  }

  @Bean
  TopicExchange tmcEventsExchange() {
    return new TopicExchange(TmcExchange.NAME, true, false);
  }

  @Bean
  MessageConverter gateStoreAmqpMessageConverter(ObjectMapper mapper) {
    return new Jackson2JsonMessageConverter(mapper);
  }

  @Bean
  EventPublisher gateStoreEventPublisher(
      RabbitTemplate rabbitTemplate,
      @Value("${tmc.outbox.confirm-timeout-ms:2000}") long timeoutMs) {
    return new AmqpEnvelopePublisher(rabbitTemplate, TmcExchange.GATE_STATION, timeoutMs);
  }

  @Bean
  @ConditionalOnProperty(
      name = "tmc.outbox.relay-enabled",
      havingValue = "true",
      matchIfMissing = true)
  GateStoreOutboxRelayJob gateStoreOutboxRelayJob(OutboxRelay gateStoreOutboxRelay) {
    return new GateStoreOutboxRelayJob(gateStoreOutboxRelay);
  }
}
