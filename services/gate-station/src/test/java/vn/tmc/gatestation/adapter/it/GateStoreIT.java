package vn.tmc.gatestation.adapter.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.gatestation.adapter.out.inmem.ProgrammableQrValidation;
import vn.tmc.gatestation.adapter.out.persistence.TransactionalGateManagement;
import vn.tmc.gatestation.application.port.in.IGateAccess;
import vn.tmc.gatestation.application.port.in.IGateAccess.Outcome;
import vn.tmc.gatestation.application.port.in.IGateAccess.ScanRequest;
import vn.tmc.gatestation.application.port.in.IGateManagement;
import vn.tmc.gatestation.application.port.in.IGateManualAccess;
import vn.tmc.gatestation.application.port.in.IGateManualAccess.ManualOpenRequest;
import vn.tmc.gatestation.application.port.out.IAuditLog;
import vn.tmc.gatestation.application.port.out.IGateConfigRepository;
import vn.tmc.gatestation.application.usecase.AdminGateService;
import vn.tmc.gatestation.bootstrap.GateStationApplication;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.domain.GateConfig;
import vn.tmc.gatestation.domain.GateMode;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.ReasonCode;
import vn.tmc.platform.messaging.AuditEvents;
import vn.tmc.platform.messaging.TmcExchange;
import vn.tmc.platform.messaging.outbox.OutboxRelay;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.testsupport.TmcContainers;

/**
 * The "gate-store" profile (ADR-14) against a real PostgreSQL: the service starts, Flyway applies
 * db/gate-store, and audit events relay to RabbitMQ. Needs Docker.
 */
@Tag("integration")
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = GateStationApplication.class)
@ActiveProfiles({"inmem", "gate-store"})
class GateStoreIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();
  @Container static final RabbitMQContainer RABBIT = TmcContainers.rabbit();

  @DynamicPropertySource
  static void db(DynamicPropertyRegistry r) {
    r.add("tmc.gate.store.url", POSTGRES::getJdbcUrl);
    r.add("tmc.gate.store.username", POSTGRES::getUsername);
    r.add("tmc.gate.store.password", POSTGRES::getPassword);
    r.add("spring.rabbitmq.host", RABBIT::getHost);
    r.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    r.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
    r.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    r.add("tmc.outbox.relay-enabled", () -> "false");
  }

  @Autowired JdbcTemplate gateStoreJdbc;
  @Autowired Flyway gateStoreFlyway;
  @Autowired IGateConfigRepository gates;
  @Autowired IGateManagement adminGates;
  @Autowired IGateManualAccess manualOpen;
  @Autowired IAuditLog audit;
  @Autowired PlatformTransactionManager gateStoreTransactions;
  @Autowired OutboxRelay outboxRelay;
  @Autowired RabbitAdmin rabbitAdmin;
  @Autowired RabbitTemplate rabbit;
  @Autowired IGateAccess scans;
  @Autowired ProgrammableQrValidation qr;
  @Autowired MockMvc mvc;

  private static final Principal SYSTEM_ADMIN =
      new Principal(UUID.randomUUID(), Set.of(Role.SYSTEM_ADMIN), DataScope.all());

  @BeforeEach
  void clearOutbox() {
    gateStoreJdbc.update("DELETE FROM outbox_events");
  }

  @Test
  void should_migrate_on_start_and_store_gates_with_valid_modes_only() {
    assertThat(gateStoreFlyway.info().current().getVersion().getVersion()).isEqualTo("2");

    gateStoreJdbc.update(
        "INSERT INTO gates (gate_id, factory_code, hardware_gate_id, mode) VALUES (?, ?, ?, ?)",
        "BD-GATE-09",
        "BD",
        "BD-HW-09",
        "BIDIRECTIONAL");
    assertThat(
            gateStoreJdbc.queryForObject(
                "SELECT maintenance FROM gates WHERE gate_id = 'BD-GATE-09'", Boolean.class))
        .isFalse();

    assertThatThrownBy(
            () ->
                gateStoreJdbc.update(
                    "INSERT INTO gates (gate_id, factory_code, hardware_gate_id, mode) "
                        + "VALUES ('X', 'BD', 'HW-X', 'SIDEWAYS')"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                gateStoreJdbc.update(
                    "INSERT INTO gates (gate_id, factory_code, hardware_gate_id, mode) "
                        + "VALUES ('Y', 'BD', 'BD-HW-09', 'ENTRY_ONLY')"))
        .as("one turnstile, one station")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void should_hold_gate_audit_events_in_the_shared_outbox_table() {
    var guard =
        new Principal(
            UUID.randomUUID(), Set.of(Role.SECURITY_GUARD), DataScope.ofGates("BD", "BD-GATE-01"));
    var employee = UUID.fromString("0192a1c0-0000-7000-8000-000000000001");
    var result =
        manualOpen.manualOpen(
            guard,
            new ManualOpenRequest(
                "BD-GATE-01", employee, Direction.ENTRY, "reader failure", "corr-audit-it"));

    assertThat(result.outcome()).isEqualTo(Outcome.ALLOWED);
    assertThat(
            gateStoreJdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE publish_status = 'PENDING'",
                Integer.class))
        .isEqualTo(1);
    var messageId =
        gateStoreJdbc.queryForObject(
            "SELECT message_id FROM outbox_events WHERE publish_status = 'PENDING'", UUID.class);
    var queueName = "gate-audit-it-" + UUID.randomUUID();
    var queue = new Queue(queueName, false, true, true);
    rabbitAdmin.declareQueue(queue);
    rabbitAdmin.declareBinding(
        BindingBuilder.bind(queue)
            .to(new TopicExchange(TmcExchange.NAME))
            .with("gate-station.AuditRecorded"));

    var report = outboxRelay.runOnce();
    assertThat(report.published()).isEqualTo(1);
    assertThat(
            gateStoreJdbc.queryForObject(
                "SELECT publish_status FROM outbox_events WHERE message_id = ?",
                String.class,
                messageId))
        .isEqualTo("PUBLISHED");
    var message = rabbit.receive(queueName, 5_000);
    assertThat(message).isNotNull();
    var envelope = (EventEnvelope) rabbit.getMessageConverter().fromMessage(message);
    assertThat(envelope.messageId()).isEqualTo(messageId);
    assertThat(envelope.eventType()).isEqualTo(AuditEvents.EVENT_TYPE);
    assertThat(envelope.source()).isEqualTo(TmcExchange.GATE_STATION);
  }

  @Test
  void should_rollback_gate_and_audit_together_when_the_use_case_fails_after_outbox_write() {
    IAuditLog failAfterWriting =
        (action, actor, target, detail) -> {
          audit.record(action, actor, target, detail);
          throw new IllegalStateException("simulated failure after audit insert");
        };
    var useCase = new AdminGateService(gates, failAfterWriting);
    var transactional =
        new TransactionalGateManagement(useCase, new TransactionTemplate(gateStoreTransactions));
    var gate = new GateConfig("BD-GATE-99", "BD", "BD-HW-99", GateMode.ENTRY_ONLY, false);

    assertThatThrownBy(() -> transactional.upsert(SYSTEM_ADMIN, gate))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("simulated failure");

    assertThat(gates.find("BD-GATE-99")).isEmpty();
    assertThat(gateStoreJdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class))
        .isZero();
  }

  @Test
  void should_seed_the_catalog_and_persist_admin_upserts() {
    assertThat(gates.find("BD-GATE-01")).isPresent();

    var gate = new GateConfig("BD-GATE-19", "BD", "BD-HW-19", GateMode.ENTRY_ONLY, false);
    assertThat(gates.upsert(gate)).isEqualTo(gate);
    assertThat(gates.find("BD-GATE-19")).contains(gate);

    var inMaintenance = new GateConfig("BD-GATE-19", "BD", "BD-HW-19", GateMode.EXIT_ONLY, true);
    assertThat(gates.upsert(inMaintenance)).isEqualTo(inMaintenance);
    assertThat(gates.find("BD-GATE-19")).contains(inMaintenance);
  }

  @Test
  void should_manage_gates_through_the_admin_api_and_enforce_system_admin() throws Exception {
    var subject = UUID.randomUUID().toString();
    var body =
        """
        {"factoryCode":"BD","hardwareGateId":"BD-HW-20","mode":"EXIT_ONLY","maintenance":false}
        """;

    mvc.perform(
            put("/admin/v1/gates/BD-GATE-20")
                .header("X-Subject", subject)
                .header("X-Roles", "SYSTEM_ADMIN")
                .header("X-Scope-Factories", "*")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gateId").value("BD-GATE-20"))
        .andExpect(jsonPath("$.mode").value("EXIT_ONLY"));

    mvc.perform(
            put("/admin/v1/gates/BD-GATE-20")
                .header("X-Subject", subject)
                .header("X-Roles", "SYSTEM_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"factoryCode\":\"BD\",\"hardwareGateId\":\"BD-HW-20\",\"mode\":\"ENTRY_ONLY\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mode").value("ENTRY_ONLY"));

    mvc.perform(
            get("/admin/v1/gates").header("X-Subject", subject).header("X-Roles", "SYSTEM_ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.gateId=='BD-GATE-20')]").isNotEmpty());

    mvc.perform(
            patch("/admin/v1/gates/BD-GATE-20/maintenance")
                .header("X-Subject", subject)
                .header("X-Roles", "SYSTEM_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maintenance\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.maintenance").value(true));

    mvc.perform(
            get("/admin/v1/gates/BD-GATE-20")
                .header("X-Subject", subject)
                .header("X-Roles", "SYSTEM_ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mode").value("ENTRY_ONLY"))
        .andExpect(jsonPath("$.maintenance").value(true));

    assertThat(gates.find("BD-GATE-20").orElseThrow().maintenance()).isTrue();
    assertThat(
            gateStoreJdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE envelope->'payload'->>'target' = ? "
                    + "AND envelope->'payload'->>'action' IN ('GATE_UPSERTED', 'GATE_MAINTENANCE_CHANGED')",
                Integer.class,
                "BD-GATE-20"))
        .isEqualTo(3);

    mvc.perform(
            put("/admin/v1/gates/BD-GATE-21")
                .header("X-Subject", subject)
                .header("X-Roles", "SECURITY_GUARD")
                .header("X-Scope-Factories", "*")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"factoryCode\":\"BD\",\"hardwareGateId\":\"BD-HW-21\",\"mode\":\"ENTRY_ONLY\"}"))
        .andExpect(status().isForbidden());

    mvc.perform(
            put("/admin/v1/gates/BD-GATE-21")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"factoryCode\":\"BD\",\"hardwareGateId\":\"BD-HW-21\",\"mode\":\"ENTRY_ONLY\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void should_reject_the_next_scan_as_gate_in_maintenance() {
    var gate = new GateConfig("BD-GATE-22", "BD", "BD-HW-22", GateMode.ENTRY_ONLY, true);
    adminGates.upsert(SYSTEM_ADMIN, gate);

    var token = UUID.randomUUID().toString();
    qr.accept(
        token,
        UUID.fromString("0192f000-0000-7000-8000-000000000001"),
        "jti-" + UUID.randomUUID(),
        Instant.now().plusSeconds(30));

    var result =
        scans.scan(
            new ScanRequest(
                UUID.randomUUID(), "BD-GATE-22", token, Instant.now(), "KIOSK-1", "corr-it"));

    assertThat(result.outcome()).isEqualTo(Outcome.DENIED);
    assertThat(result.reason()).isEqualTo(ReasonCode.GATE_IN_MAINTENANCE);
  }
}
