package vn.tmc.reporting.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.ProcessedMessageStore;
import vn.tmc.platform.testsupport.TmcContainers;
import vn.tmc.reporting.adapter.out.persistence.JdbcPresenceRepository;
import vn.tmc.reporting.application.port.out.IFraudAlertStore;
import vn.tmc.reporting.application.port.out.IPresenceRepository;
import vn.tmc.reporting.application.usecase.ProjectAttendanceEventUseCase;
import vn.tmc.reporting.bootstrap.ReportingApplication;

/**
 * D-04: the presence read model is in PostgreSQL and shares the dedup transaction. Presence
 * survives a restart of reporting, and a handler failure leaves neither the projection row nor the
 * processed_messages marker. Needs Docker: ./gradlew :services:reporting:integrationTest
 */
@Tag("integration")
@Testcontainers
class PresencePersistenceIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();
  @Container static final RabbitMQContainer RABBIT = TmcContainers.rabbit();

  private static ConfigurableApplicationContext startReporting() {
    // command-line arguments, so they win over application.yml
    return new SpringApplicationBuilder(ReportingApplication.class)
        .run(
            "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
            "--spring.datasource.username=" + POSTGRES.getUsername(),
            "--spring.datasource.password=" + POSTGRES.getPassword(),
            "--spring.rabbitmq.host=" + RABBIT.getHost(),
            "--spring.rabbitmq.port=" + RABBIT.getAmqpPort(),
            "--spring.rabbitmq.username=" + RABBIT.getAdminUsername(),
            "--spring.rabbitmq.password=" + RABBIT.getAdminPassword(),
            "--server.port=0");
  }

  private static EventEnvelope confirmed(UUID employee, String direction, String scannedAt) {
    return new EventEnvelope(
        Ids.uuidV7(),
        "AttendanceConfirmed",
        1,
        Ids.uuidV7(),
        5,
        Instant.parse(scannedAt),
        "it",
        "attendance-edge",
        "BD",
        "BD-GATE-01",
        Map.of(
            "employeeUuid", employee.toString(), "direction", direction, "scannedAt", scannedAt));
  }

  private static int count(JdbcTemplate jdbc, String sql, Object arg) {
    return jdbc.queryForObject(sql, Integer.class, arg);
  }

  @Test
  void should_keep_presence_across_a_restart_of_reporting() {
    var employee = UUID.randomUUID();
    try (var reporting = startReporting()) {
      assertThat(reporting.getBean(IPresenceRepository.class))
          .isInstanceOf(JdbcPresenceRepository.class);
      reporting
          .getBean(IdempotentConsumer.class)
          .accept(confirmed(employee, "ENTRY", "2026-11-20T07:00:00Z"));
    }

    try (var reporting = startReporting()) {
      var presence = reporting.getBean(IPresenceRepository.class).find(employee, "BD");
      assertThat(presence).isPresent();
      assertThat(presence.get().direction()).isEqualTo("ENTRY");
      assertThat(reporting.getBean(IPresenceRepository.class).byFactory("BD"))
          .anySatisfy(p -> assertThat(p.employeeUuid()).isEqualTo(employee));
    }
  }

  @Test
  void should_store_an_impossible_travel_alert_once_and_keep_it_across_a_restart() {
    var employee = UUID.randomUUID();
    var bd = confirmed(employee, "ENTRY", "2026-11-21T06:45:00Z");
    var la =
        new EventEnvelope(
            Ids.uuidV7(),
            "AttendanceConfirmed",
            1,
            Ids.uuidV7(),
            5,
            Instant.parse("2026-11-21T06:55:00Z"),
            "it",
            "attendance-edge",
            "LA",
            "LA-GATE-01",
            Map.of(
                "employeeUuid",
                employee.toString(),
                "direction",
                "ENTRY",
                "scannedAt",
                "2026-11-21T06:55:00Z"));
    try (var reporting = startReporting()) {
      var consumer = reporting.getBean(IdempotentConsumer.class);
      consumer.accept(bd);
      consumer.accept(la);
      consumer.accept(la); // redelivery
    }

    try (var reporting = startReporting()) {
      var alerts = reporting.getBean(IFraudAlertStore.class).involving("LA", 50);
      assertThat(alerts)
          .filteredOn(a -> a.employeeUuid().equals(employee))
          .singleElement()
          .satisfies(
              a -> {
                assertThat(a.attemptId()).isEqualTo(la.eventId());
                assertThat(a.otherFactory()).isEqualTo("BD");
              });
    }
  }

  @Test
  void should_roll_back_the_projection_and_the_marker_when_the_handler_fails() {
    var employee = UUID.randomUUID();
    var event = confirmed(employee, "ENTRY", "2026-11-20T08:00:00Z");
    try (var reporting = startReporting()) {
      var store = reporting.getBean(ProcessedMessageStore.class);
      var tx = reporting.getBean(IdempotentConsumer.TransactionRunner.class);
      var projection = reporting.getBean(ProjectAttendanceEventUseCase.class);
      var failing =
          new IdempotentConsumer(
              "reporting",
              store,
              e -> {
                projection.handle(e); // the row is written ...
                throw new IllegalStateException("crash after the projection write");
              },
              tx);

      assertThatThrownBy(() -> failing.accept(event)).isInstanceOf(IllegalStateException.class);

      var jdbc = reporting.getBean(JdbcTemplate.class);
      assertThat(
              count(
                  jdbc,
                  "SELECT count(*) FROM attendance_projection WHERE employee_uuid = ?",
                  employee))
          .isZero();
      assertThat(
              count(
                  jdbc,
                  "SELECT count(*) FROM processed_messages WHERE message_id = ?",
                  event.messageId()))
          .isZero();

      // ... and the redelivery is projected normally
      assertThat(reporting.getBean(IdempotentConsumer.class).accept(event)).isTrue();
      assertThat(reporting.getBean(IPresenceRepository.class).find(employee, "BD")).isPresent();
    }
  }
}
