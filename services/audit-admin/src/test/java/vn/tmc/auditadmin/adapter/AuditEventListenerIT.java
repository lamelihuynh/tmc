package vn.tmc.auditadmin.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.auditadmin.application.port.out.IAuditStore;
import vn.tmc.auditadmin.bootstrap.AuditAdminApplication;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.messaging.TmcExchange;
import vn.tmc.platform.testsupport.TmcContainers;

/**
 * Audit Admin consumes *.AuditRecorded from tmc.events. Real PostgreSQL (Flyway) and RabbitMQ: the
 * listener, the tmc.events bindings and the JDBC dedup store. Needs Docker: ./gradlew
 * :services:audit-admin:integrationTest
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(classes = AuditAdminApplication.class)
class AuditEventListenerIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();
  @Container static final RabbitMQContainer RABBIT = TmcContainers.rabbit();

  @DynamicPropertySource
  static void infra(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
    r.add("spring.flyway.user", POSTGRES::getUsername);
    r.add("spring.flyway.password", POSTGRES::getPassword);
    r.add("spring.rabbitmq.host", RABBIT::getHost);
    r.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    r.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
    r.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
  }

  @Autowired RabbitTemplate rabbit;
  @Autowired JdbcTemplate jdbc;

  private void publish(String source, EventEnvelope e) {
    rabbit.convertAndSend(TmcExchange.NAME, TmcExchange.routingKey(source, e.eventType()), e);
  }

  private int processed(UUID messageId) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM processed_messages WHERE message_id = ?", Integer.class, messageId);
  }

  private static EventEnvelope event(
      String type, UUID eventId, long version, Map<String, Object> payload) {
    return new EventEnvelope(
        Ids.uuidV7(),
        type,
        1,
        eventId,
        version,
        Instant.now(),
        "it",
        "attendance-edge",
        "BD",
        "BD-GATE-01",
        payload);
  }

  @Autowired IAuditStore store;

  @Test
  void should_record_audit_events_from_every_source_once() {
    var before = store.count();
    var fromUserQr = event("AuditRecorded", Ids.uuidV7(), 1, Map.of("action", "DEVICE_REVOKED"));
    var fromIdentity = event("AuditRecorded", Ids.uuidV7(), 1, Map.of("action", "LOGIN_LOCKED"));
    var fromAttendance = event("AuditRecorded", Ids.uuidV7(), 1, Map.of("action", "ACCESS_DENIED"));

    publish("user-qr", fromUserQr);
    publish("identity", fromIdentity);
    publish("attendance", fromAttendance);
    publish("user-qr", fromUserQr); // redelivery
    // not an audit event: not bound to the audit queue
    publish("attendance", event("AttendanceConfirmed", Ids.uuidV7(), 1, Map.of()));

    await().atMost(Duration.ofSeconds(30)).until(() -> store.count() == before + 3);
    await().atMost(Duration.ofSeconds(10)).until(() -> processed(fromUserQr.messageId()) == 1);
    assertThat(store.search(null, null, "LOGIN_LOCKED", null, 10)).hasSize(1);
  }
}
