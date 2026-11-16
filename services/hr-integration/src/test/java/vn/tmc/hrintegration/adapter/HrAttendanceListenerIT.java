package vn.tmc.hrintegration.adapter;

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
import vn.tmc.hrintegration.adapter.out.inmem.SimulatedHrPayrollAdapter;
import vn.tmc.hrintegration.application.port.out.ISyncLogRepository;
import vn.tmc.hrintegration.bootstrap.HrIntegrationApplication;
import vn.tmc.hrintegration.domain.SyncStatus;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.messaging.TmcExchange;
import vn.tmc.platform.testsupport.TmcContainers;

/**
 * HR Integration consumes AttendanceConfirmed and AttendanceCorrected from tmc.events. Real
 * PostgreSQL (Flyway) and RabbitMQ: the listener, the tmc.events bindings and the JDBC dedup store.
 * Needs Docker: ./gradlew :services:hr-integration:integrationTest
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(classes = HrIntegrationApplication.class)
class HrAttendanceListenerIT {
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

  @Autowired ISyncLogRepository log;
  @Autowired SimulatedHrPayrollAdapter hr;

  @Test
  void should_send_a_confirmed_attendance_to_hr_once() {
    var attempt = Ids.uuidV7();
    var confirmed =
        event(
            "AttendanceConfirmed",
            attempt,
            3,
            Map.of(
                "employeeUuid",
                UUID.randomUUID().toString(),
                "direction",
                "ENTRY",
                "scannedAt",
                "2026-11-20T07:00:00Z"));

    publish("attendance", confirmed);
    publish("attendance", confirmed);

    await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(
            () -> assertThat(log.find(attempt).map(r -> r.status())).contains(SyncStatus.SYNCED));
    await().atMost(Duration.ofSeconds(10)).until(() -> processed(confirmed.messageId()) == 1);
    assertThat(hr.received()).filteredOn(s -> s.eventId().equals(attempt)).hasSize(1);
  }

  @Test
  void should_park_a_message_that_keeps_failing_in_the_dead_letter_queue() {
    // no employeeUuid: the use case cannot build the HR submission and throws every time
    var poison = event("AttendanceConfirmed", Ids.uuidV7(), 1, Map.of("direction", "ENTRY"));

    publish("attendance", poison);

    await()
        .atMost(Duration.ofSeconds(30))
        .until(() -> rabbit.receive("tmc.hr.attendance.dlq", 200) != null);
    assertThat(processed(poison.messageId())).isZero(); // marker rolled back with the failure
  }
}
