package vn.tmc.platform.messaging.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.messaging.AuditEvents;
import vn.tmc.platform.messaging.EventPublisher;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.TmcExchange;
import vn.tmc.platform.messaging.amqp.AmqpEnvelopePublisher;
import vn.tmc.platform.messaging.jdbc.JdbcOutboxStore;
import vn.tmc.platform.messaging.jdbc.JdbcProcessedMessageStore;
import vn.tmc.platform.messaging.outbox.OutboxRelay;

/**
 * The shared outbox, AMQP publisher and dedup store against real PostgreSQL and RabbitMQ: outbox
 * row, relay, publisher confirm, the tmc.events exchange with a {@code *.AuditRecorded} binding,
 * then an idempotent consumer on a JDBC dedup store. Needs Docker: ./gradlew
 * :platform:messaging:integrationTest
 */
@Tag("integration")
@Testcontainers
class MessagingInfraIT {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  static final RabbitMQContainer RABBIT =
      new RabbitMQContainer("rabbitmq:3.13-management-alpine")
          // RabbitMQ takes ~45 s to boot on an arm64 laptop, more than the default wait
          .withStartupTimeout(Duration.ofMinutes(3));

  static JdbcTemplate jdbc;
  static TransactionTemplate tx;
  static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @BeforeAll
  static void schema() {
    DataSource ds =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    jdbc = new JdbcTemplate(ds);
    jdbc.execute(JdbcOutboxStore.DDL);
    jdbc.execute(JdbcProcessedMessageStore.DDL);
    tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
  }

  private static CachingConnectionFactory connectionFactory() {
    var cf = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getAmqpPort());
    cf.setUsername(RABBIT.getAdminUsername());
    cf.setPassword(RABBIT.getAdminPassword());
    cf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
    return cf;
  }

  private static EventEnvelope audit(String source, String action) {
    return AuditEvents.recorded(
        source, UUID.randomUUID(), action, "target-1", "OK", null, "corr-it", Instant.now());
  }

  @Test
  void should_mark_a_message_processed_once_per_consumer() {
    var store = new JdbcProcessedMessageStore(jdbc);
    var id = UUID.randomUUID();

    assertThat(store.markProcessed("reporting", id)).isTrue();
    assertThat(store.markProcessed("reporting", id)).isFalse();
    assertThat(store.markProcessed("audit-admin", id)).isTrue(); // another consumer, own row
  }

  @Test
  void should_roll_the_dedup_marker_back_when_the_handler_fails_in_the_same_transaction() {
    var store = new JdbcProcessedMessageStore(jdbc);
    var calls = new ArrayList<UUID>();
    var failOnce = new boolean[] {true};
    var consumer =
        new IdempotentConsumer(
            "rollback-test",
            store,
            e -> {
              if (failOnce[0]) {
                failOnce[0] = false;
                throw new IllegalStateException("read model write failed");
              }
              calls.add(e.messageId());
            },
            work -> Boolean.TRUE.equals(tx.execute(s -> work.getAsBoolean())));
    var e = audit("identity", "LOGIN_FAILED");

    assertThatThrownBy(() -> consumer.accept(e)).isInstanceOf(IllegalStateException.class);
    assertThat(consumer.accept(e)).isTrue(); // the redelivery is processed: marker was rolled back
    assertThat(consumer.accept(e)).isFalse();
    assertThat(calls).containsExactly(e.messageId());
  }

  @Test
  void should_keep_outbox_order_and_failure_counts_in_postgres() {
    var outbox = new JdbcOutboxStore(jdbc, MAPPER);
    var a = audit("user-qr", "A");
    var b = audit("user-qr", "B");
    outbox.add(a);
    outbox.add(a); // same messageId again: ignored
    outbox.add(b);
    outbox.recordFailure(a.messageId());

    var pending = outbox.fetchPending(1000);
    assertThat(pending)
        .extracting(EventEnvelope::messageId)
        .containsSubsequence(a.messageId(), b.messageId());
    assertThat(pending.stream().filter(p -> p.messageId().equals(a.messageId())).count())
        .isEqualTo(1);
    assertThat(
            pending
                .get(
                    pending.indexOf(
                        pending.stream()
                            .filter(p -> p.messageId().equals(a.messageId()))
                            .findFirst()
                            .orElseThrow()))
                .payload())
        .containsEntry("action", "A");
    assertThat(
            jdbc.queryForObject(
                "SELECT failure_count FROM outbox_events WHERE message_id = ?",
                Integer.class,
                a.messageId()))
        .isEqualTo(1);
  }

  @Test
  void should_round_trip_outbox_to_rabbit_to_an_idempotent_consumer() throws Exception {
    var cf = connectionFactory();
    var admin = new RabbitAdmin(cf);
    var exchange = new TopicExchange(TmcExchange.NAME, true, false);
    var queue = new Queue("it.audit." + UUID.randomUUID(), false, false, false);
    admin.declareExchange(exchange);
    admin.declareQueue(queue);
    admin.declareBinding(
        BindingBuilder.bind(queue).to(exchange).with(TmcExchange.anySource("AuditRecorded")));
    var otherQueue = new Queue("it.reporting." + UUID.randomUUID(), false, false, false);
    admin.declareQueue(otherQueue);
    admin.declareBinding(
        BindingBuilder.bind(otherQueue).to(exchange).with(TmcExchange.everyEventOf("attendance")));

    var rabbit = new RabbitTemplate(cf);
    rabbit.setMessageConverter(new Jackson2JsonMessageConverter(MAPPER));
    var outbox = new JdbcOutboxStore(jdbc, MAPPER);
    var relay = new OutboxRelay(outbox, new AmqpEnvelopePublisher(rabbit, "user-qr", 5000), 100);
    var sent = audit("user-qr", "DEVICE_REVOKED");
    outbox.add(sent);

    var report = relay.runOnce();
    assertThat(report.published()).isGreaterThanOrEqualTo(1);
    assertThat(outbox.fetchPending(1000))
        .extracting(EventEnvelope::messageId)
        .doesNotContain(sent.messageId());

    var received = new ArrayList<EventEnvelope>();
    var consumer =
        new IdempotentConsumer(
            "audit-admin-it", new JdbcProcessedMessageStore(jdbc), received::add);
    EventEnvelope got = null;
    for (int i = 0; i < 50 && got == null; i++) {
      var m = rabbit.receiveAndConvert(queue.getName(), 200);
      if (m instanceof EventEnvelope e && e.messageId().equals(sent.messageId())) {
        got = e;
      }
    }
    assertThat(got).as("message routed by user-qr.AuditRecorded to *.AuditRecorded").isNotNull();
    assertThat(got.payload()).containsEntry("action", "DEVICE_REVOKED");
    assertThat(got.occurredAt()).isEqualTo(sent.occurredAt());
    assertThat(consumer.accept(got)).isTrue();
    assertThat(consumer.accept(got)).isFalse(); // a redelivery is a no-op
    assertThat(received).hasSize(1);
    assertThat(rabbit.receive(otherQueue.getName(), 200))
        .as("attendance.* must not get it")
        .isNull();
    cf.destroy();
  }

  @Test
  void should_report_not_published_when_the_broker_is_unreachable() {
    var cf = new CachingConnectionFactory("localhost", 1); // nothing listens there
    cf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
    var rabbit = new RabbitTemplate(cf);
    rabbit.setMessageConverter(new Jackson2JsonMessageConverter(MAPPER));
    var publisher = new AmqpEnvelopePublisher(rabbit, "identity", 1000);

    assertThatThrownBy(() -> publisher.publish(audit("identity", "LOGIN_LOCKED")))
        .isInstanceOf(EventPublisher.PublishFailedException.class);
    cf.destroy();
  }
}
