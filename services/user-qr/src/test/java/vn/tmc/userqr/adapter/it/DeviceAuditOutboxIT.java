package vn.tmc.userqr.adapter.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.messaging.TmcExchange;
import vn.tmc.platform.messaging.jdbc.JdbcOutboxStore;
import vn.tmc.platform.messaging.outbox.AuditOutbox;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.testsupport.TmcContainers;
import vn.tmc.userqr.adapter.out.messaging.OutboxAuditLog;
import vn.tmc.userqr.adapter.out.persistence.JdbcDeviceKeyRegistry;
import vn.tmc.userqr.adapter.out.persistence.SpringTransactionRunner;
import vn.tmc.userqr.application.port.out.IAuditLog;
import vn.tmc.userqr.application.usecase.DeviceManagementService;
import vn.tmc.userqr.support.PhoneSimulator;

/**
 * C-08: a device change and its AuditRecorded outbox row commit together, on PostgreSQL with the
 * adapters of the default profile. Needs Docker: ./gradlew :services:user-qr:integrationTest
 */
@Tag("integration")
@Testcontainers
class DeviceAuditOutboxIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();

  static JdbcTemplate jdbc;
  static JdbcDeviceKeyRegistry registry;
  static SpringTransactionRunner tx;
  static OutboxAuditLog outboxAudit;

  @BeforeAll
  static void wire() {
    DataSource ds =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    Flyway.configure().dataSource(ds).load().migrate();
    jdbc = new JdbcTemplate(ds);
    registry = new JdbcDeviceKeyRegistry(jdbc);
    tx = new SpringTransactionRunner(new TransactionTemplate(new DataSourceTransactionManager(ds)));
    var mapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    outboxAudit =
        new OutboxAuditLog(
            new AuditOutbox(
                new JdbcOutboxStore(jdbc, mapper), TmcExchange.USER_QR, IClock.system()));
  }

  private static DeviceManagementService service(IAuditLog audit) {
    return new DeviceManagementService(registry, audit, tx, IClock.system());
  }

  private static Principal employee(UUID id) {
    return new Principal(id, Set.of(Role.EMPLOYEE), DataScope.all());
  }

  private static int outboxRows() {
    return jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class);
  }

  private static int outboxRowsFor(String action, String target) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM outbox_events WHERE event_type = 'AuditRecorded' "
            + "AND envelope->'payload'->>'action' = ? AND envelope->'payload'->>'target' = ? "
            + "AND envelope->>'source' = 'user-qr' AND publish_status = 'PENDING'",
        Integer.class,
        action,
        target);
  }

  private static int devicesOf(UUID employee) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM devices WHERE employee_uuid = ?", Integer.class, employee);
  }

  @Test
  void should_write_exactly_one_outbox_row_when_a_device_is_registered() {
    var id = UUID.randomUUID();
    var phone = new PhoneSimulator(id);
    var before = outboxRows();

    var deviceId = service(outboxAudit).register(employee(id), id, phone.publicKeyBase64());

    assertThat(outboxRows()).isEqualTo(before + 1);
    assertThat(outboxRowsFor("DEVICE_REGISTER", deviceId.deviceId())).isEqualTo(1);
    var actor =
        jdbc.queryForObject(
            "SELECT envelope->'payload'->>'actor' FROM outbox_events ORDER BY seq DESC LIMIT 1",
            String.class);
    var envelope =
        jdbc.queryForObject(
            "SELECT envelope::text FROM outbox_events ORDER BY seq DESC LIMIT 1", String.class);
    assertThat(actor).isEqualTo(id.toString());
    // identifiers only, never the key (ADR-09)
    assertThat(envelope).doesNotContain(phone.publicKeyBase64());
  }

  @Test
  void should_write_one_row_when_a_new_phone_replaces_the_active_one() {
    var id = UUID.randomUUID();
    var service = service(outboxAudit);
    var first = service.register(employee(id), id, new PhoneSimulator(id).publicKeyBase64());
    var before = outboxRows();

    var second = service.register(employee(id), id, new PhoneSimulator(id).publicKeyBase64());

    assertThat(outboxRows()).isEqualTo(before + 1);
    assertThat(outboxRowsFor("DEVICE_REGISTER", second.deviceId())).isEqualTo(1);
    assertThat(registry.findById(first.deviceId()).orElseThrow().isActive()).isFalse();
    assertThat(registry.findActiveByEmployee(id).orElseThrow().deviceId())
        .isEqualTo(second.deviceId());
  }

  @Test
  void should_keep_neither_the_device_nor_the_outbox_row_when_registration_fails_midway() {
    var id = UUID.randomUUID();
    var old =
        service(outboxAudit).register(employee(id), id, new PhoneSimulator(id).publicKeyBase64());
    var before = outboxRows();
    // the outbox row is written, then the process "crashes" before the commit
    IAuditLog crashAfterOutbox =
        (action, actor, target, result) -> {
          outboxAudit.record(action, actor, target, result);
          throw new IllegalStateException("simulated failure after the outbox write");
        };

    assertThatThrownBy(
            () ->
                service(crashAfterOutbox)
                    .register(employee(id), id, new PhoneSimulator(id).publicKeyBase64()))
        .isInstanceOf(IllegalStateException.class);

    assertThat(outboxRows()).isEqualTo(before);
    assertThat(devicesOf(id)).isEqualTo(1); // no new device
    assertThat(registry.findActiveByEmployee(id).orElseThrow().deviceId())
        .as("the revocation of the old phone was rolled back too")
        .isEqualTo(old.deviceId());
  }

  @Test
  void should_write_the_revocation_by_hr_and_its_audit_row_together() {
    var id = UUID.randomUUID();
    var device =
        service(outboxAudit).register(employee(id), id, new PhoneSimulator(id).publicKeyBase64());
    var hr = new Principal(UUID.randomUUID(), Set.of(Role.HR_STAFF), DataScope.all());

    service(outboxAudit).revokeForEmployee(hr, id);

    assertThat(registry.findActiveByEmployee(id)).isEmpty();
    assertThat(outboxRowsFor("DEVICE_REVOKE_BY_STAFF", device.deviceId())).isEqualTo(1);
  }

  @Test
  void should_keep_the_denied_audit_row_although_the_request_is_refused() {
    var caller = UUID.randomUUID();
    var victim = UUID.randomUUID();
    var before = outboxRows();

    assertThatThrownBy(
            () ->
                service(outboxAudit)
                    .register(
                        employee(caller), victim, new PhoneSimulator(victim).publicKeyBase64()))
        .isInstanceOf(vn.tmc.platform.security.AccessDeniedException.class);

    assertThat(outboxRows()).isEqualTo(before + 1);
    assertThat(
            jdbc.queryForObject(
                "SELECT envelope->'payload'->>'result' FROM outbox_events ORDER BY seq DESC LIMIT 1",
                String.class))
        .isEqualTo("DENIED");
    assertThat(devicesOf(victim)).isZero();
  }
}
