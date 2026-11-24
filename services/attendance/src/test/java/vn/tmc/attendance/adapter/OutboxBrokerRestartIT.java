package vn.tmc.attendance.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.dockerjava.api.model.ExposedPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
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
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.attendance.adapter.out.persistence.JdbcOutboxRepository;
import vn.tmc.attendance.application.usecase.PublishOutboxUseCase;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.messaging.TmcExchange;
import vn.tmc.platform.messaging.amqp.AmqpEnvelopePublisher;
import vn.tmc.platform.testsupport.TmcContainers;

/**
 * B-02: proves ADR-11 point P7 against real PostgreSQL and RabbitMQ when the broker stops and
 * restarts mid-publish. Needs Docker: ./gradlew :services:attendance:integrationTest
 */
@Tag("integration")
@Testcontainers
class OutboxBrokerRestartIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();

  @Container static final RabbitMQContainer RABBIT = TmcContainers.rabbit();

  static JdbcTemplate jdbc;
  static CachingConnectionFactory connectionFactory;
  static RabbitTemplate rabbitTemplate;
  static String queueName;

  static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @BeforeAll
  static void setUp() {
    DataSource ds =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    Flyway.configure().dataSource(ds).load().migrate();
    jdbc = new JdbcTemplate(ds);

    connectionFactory = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getAmqpPort());
    connectionFactory.setUsername(RABBIT.getAdminUsername());
    connectionFactory.setPassword(RABBIT.getAdminPassword());
    connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);

    var admin = new RabbitAdmin(connectionFactory);
    var exchange = new TopicExchange(TmcExchange.NAME, true, false);
    queueName = "it.attendance.restart." + UUID.randomUUID();
    var queue = new Queue(queueName, true, false, false);
    admin.declareExchange(exchange);
    admin.declareQueue(queue);
    admin.declareBinding(
        BindingBuilder.bind(queue)
            .to(exchange)
            .with(TmcExchange.everyEventOf(TmcExchange.ATTENDANCE)));

    rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter(MAPPER));
  }

  @AfterAll
  static void tearDown() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
  }

  @Test
  void should_keep_outbox_pending_while_broker_is_stopped_and_deliver_in_order_once_restarted()
      throws Exception {
    var outbox = new JdbcOutboxRepository(jdbc, MAPPER);
    var publisher = new AmqpEnvelopePublisher(rabbitTemplate, TmcExchange.ATTENDANCE, 2000);
    var useCase = new PublishOutboxUseCase(outbox, publisher, 100);

    // Pre-existing PUBLISHED row must not be re-published after broker restart
    var alreadyPublished = envelope("AccessAttemptRecorded");
    outbox.add(alreadyPublished);
    var initialRun = useCase.run();
    assertThat(initialRun.published()).isEqualTo(1);
    assertThat(publishStatus(alreadyPublished.messageId())).isEqualTo("PUBLISHED");
    var initialReceived = receiveMessages(1);
    assertThat(initialReceived)
        .extracting(EventEnvelope::messageId)
        .containsExactly(alreadyPublished.messageId());

    // Case 1: RabbitMQ stops -> pending events stay PENDING and are not marked PUBLISHED
    var first = envelope("AccessAttemptRecorded");
    var second = envelope("GateResultRecorded");
    var third = envelope("AttendanceConfirmed");
    outbox.add(first);
    outbox.add(second);
    outbox.add(third);

    stopBroker();

    var whileDown = useCase.run();
    assertThat(whileDown.published()).isZero();
    assertThat(whileDown.stillPending()).isEqualTo(3);
    assertThat(publishStatus(first.messageId())).isEqualTo("PENDING");
    assertThat(publishStatus(second.messageId())).isEqualTo("PENDING");
    assertThat(publishStatus(third.messageId())).isEqualTo("PENDING");
    assertThat(outbox.fetchPending(100))
        .extracting(EventEnvelope::messageId)
        .containsExactly(first.messageId(), second.messageId(), third.messageId());

    // Case 2: RabbitMQ restarts -> all pending events arrive in insertion order, no duplicate
    restartBroker();

    var afterRestart = useCase.run();
    assertThat(afterRestart.published()).isEqualTo(3);
    assertThat(afterRestart.stillPending()).isZero();
    assertThat(publishStatus(first.messageId())).isEqualTo("PUBLISHED");
    assertThat(publishStatus(second.messageId())).isEqualTo("PUBLISHED");
    assertThat(publishStatus(third.messageId())).isEqualTo("PUBLISHED");
    assertThat(outbox.fetchPending(100)).isEmpty();

    var delivered = receiveMessages(3);
    assertThat(delivered)
        .extracting(EventEnvelope::messageId)
        .containsExactly(first.messageId(), second.messageId(), third.messageId());
    assertThat(delivered)
        .extracting(EventEnvelope::eventType)
        .containsExactly("AccessAttemptRecorded", "GateResultRecorded", "AttendanceConfirmed");
    // Already-published event was not re-published
    assertThat(rabbitTemplate.receive(queueName, 300)).isNull();
  }

  private static void stopBroker() {
    RABBIT.getDockerClient().stopContainerCmd(RABBIT.getContainerId()).exec();
    connectionFactory.resetConnection();
  }

  private static void restartBroker() throws InterruptedException {
    RABBIT.getDockerClient().startContainerCmd(RABBIT.getContainerId()).exec();
    int restartedPort =
        Integer.parseInt(
            RABBIT
                .getDockerClient()
                .inspectContainerCmd(RABBIT.getContainerId())
                .exec()
                .getNetworkSettings()
                .getPorts()
                .getBindings()
                .get(ExposedPort.tcp(5672))[0]
                .getHostPortSpec());
    connectionFactory.setPort(restartedPort);
    connectionFactory.resetConnection();

    long deadline = System.currentTimeMillis() + 60_000L;
    while (System.currentTimeMillis() < deadline) {
      try {
        Boolean open = rabbitTemplate.execute(channel -> channel.isOpen());
        if (Boolean.TRUE.equals(open)) {
          return;
        }
      } catch (Exception ignored) {
        // Broker is still booting
      }
      Thread.sleep(500);
    }
    throw new IllegalStateException("RabbitMQ did not become ready within 60s after restart");
  }

  private static List<EventEnvelope> receiveMessages(int expected) {
    var received = new ArrayList<EventEnvelope>(expected);
    for (int i = 0; i < 60 && received.size() < expected; i++) {
      var msg = rabbitTemplate.receiveAndConvert(queueName, 250);
      if (msg instanceof EventEnvelope e) {
        received.add(e);
      }
    }
    return received;
  }

  private static String publishStatus(UUID messageId) {
    return jdbc.queryForObject(
        "SELECT publish_status FROM outbox_events WHERE message_id = ?", String.class, messageId);
  }

  private static EventEnvelope envelope(String type) {
    return new EventEnvelope(
        Ids.uuidV7(), type, 1, Ids.uuidV7(), 1, Instant.now(), "corr-b02", "it", "BD", "G1", null);
  }
}
