package vn.tmc.reporting.adapter;

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
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.messaging.TmcExchange;
import vn.tmc.platform.testsupport.TmcContainers;
import vn.tmc.reporting.application.port.out.IPresenceRepository;
import vn.tmc.reporting.bootstrap.ReportingApplication;

/**
 * Reporting consumes attendance.* from tmc.events. Real PostgreSQL (Flyway) and RabbitMQ: the
 * listener, the tmc.events bindings and the JDBC dedup store. Needs Docker: ./gradlew
 * :services:reporting:integrationTest
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(classes = ReportingApplication.class)
class AttendanceEventListenerIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();
  @Container static final RabbitMQContainer RABBIT = TmcContainers.rabbit();

  @DynamicPropertySource
  static void infra(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
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

  @Autowired IPresenceRepository presences;

  @Test
  void should_project_attendance_events_once_and_apply_a_correction() {
    var employee = UUID.randomUUID();
    var attempt = Ids.uuidV7();
    var confirmed =
        event(
            "AttendanceConfirmed",
            attempt,
            5,
            Map.of(
                "employeeUuid",
                employee.toString(),
                "direction",
                "ENTRY",
                "scannedAt",
                "2026-11-20T07:00:00Z"));

    publish("attendance", confirmed);
    publish("attendance", confirmed); // redelivery with the same messageId

    await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(() -> assertThat(presences.find(employee, "BD")).isPresent());
    await().atMost(Duration.ofSeconds(10)).until(() -> processed(confirmed.messageId()) == 1);
    assertThat(presences.find(employee, "BD").orElseThrow().direction()).isEqualTo("ENTRY");

    var corrected =
        event(
            "AttendanceCorrected",
            attempt,
            6,
            Map.of(
                "status",
                "CORRECTED",
                "employeeUuid",
                employee.toString(),
                "direction",
                "EXIT",
                "scannedAt",
                "2026-11-20T07:00:00Z",
                "revision",
                1));
    publish("attendance", corrected);
    // a user-qr audit event is not bound to reporting's queue
    publish("user-qr", event("AuditRecorded", Ids.uuidV7(), 1, Map.of("action", "DEVICE_REVOKED")));

    await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(
            () ->
                assertThat(presences.find(employee, "BD").orElseThrow().direction())
                    .isEqualTo("EXIT"));
  }
}
