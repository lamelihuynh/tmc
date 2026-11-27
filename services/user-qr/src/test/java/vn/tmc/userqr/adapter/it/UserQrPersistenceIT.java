package vn.tmc.userqr.adapter.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.messaging.AuditEvents;
import vn.tmc.platform.messaging.TmcExchange;
import vn.tmc.platform.messaging.jdbc.JdbcOutboxStore;
import vn.tmc.platform.testsupport.TmcContainers;
import vn.tmc.userqr.adapter.out.persistence.JdbcDeviceKeyRegistry;
import vn.tmc.userqr.application.port.out.ActiveDeviceConflictException;
import vn.tmc.userqr.domain.Device;
import vn.tmc.userqr.domain.DeviceStatus;

/**
 * JdbcDeviceKeyRegistry and the user-qr outbox (V1, V2) against a real PostgreSQL. Needs Docker:
 * ./gradlew :services:user-qr:integrationTest
 */
@Tag("integration")
@Testcontainers
class UserQrPersistenceIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();

  static JdbcTemplate jdbc;

  @BeforeAll
  static void migrate() {
    DataSource ds =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    Flyway.configure().dataSource(ds).load().migrate();
    jdbc = new JdbcTemplate(ds);
  }

  private static PublicKey key() throws Exception {
    var g = KeyPairGenerator.getInstance("EC");
    g.initialize(new ECGenParameterSpec("secp256r1"));
    return g.generateKeyPair().getPublic();
  }

  // PostgreSQL keeps microseconds; compare at that precision
  private static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  @Test
  void should_store_a_device_read_its_key_back_and_revoke_it() throws Exception {
    var registry = new JdbcDeviceKeyRegistry(jdbc);
    var employee = UUID.randomUUID();
    var key = key();
    var t0 = now();
    var device = Device.register("dev-" + UUID.randomUUID(), employee, key, t0);

    registry.save(device);

    var loaded = registry.findById(device.deviceId()).orElseThrow();
    assertThat(loaded.publicKey().getEncoded()).isEqualTo(key.getEncoded());
    assertThat(registry.findActiveByEmployee(employee)).isPresent();

    registry.save(device.revoke(t0.plusSeconds(5)));

    assertThat(registry.findById(device.deviceId()).orElseThrow().status())
        .isEqualTo(DeviceStatus.REVOKED);
    assertThat(registry.findActiveByEmployee(employee)).isEmpty();
    assertThat(registry.changedSince(t0.plusSeconds(1)))
        .extracting(Device::deviceId)
        .contains(device.deviceId());
    assertThat(registry.changedSince(t0.plusSeconds(10)))
        .extracting(Device::deviceId)
        .doesNotContain(device.deviceId());
  }

  @Test
  void should_refuse_a_second_active_device_for_the_same_employee() throws Exception {
    var registry = new JdbcDeviceKeyRegistry(jdbc);
    var employee = UUID.randomUUID();
    registry.save(Device.register("dev-" + UUID.randomUUID(), employee, key(), now()));

    assertThatThrownBy(
            () ->
                registry.save(Device.register("dev-" + UUID.randomUUID(), employee, key(), now())))
        .isInstanceOf(ActiveDeviceConflictException.class)
        .hasCauseInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void should_keep_audit_events_in_the_outbox_table_of_v2() {
    var mapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    var outbox = new JdbcOutboxStore(jdbc, mapper);
    var e =
        AuditEvents.recorded(
            TmcExchange.USER_QR, null, "DEVICE_REVOKED", "dev-1", "OK", null, "c", Instant.now());

    outbox.add(e);
    outbox.markPublished(e.messageId());

    assertThat(outbox.fetchPending(1000))
        .extracting(EventEnvelope::messageId)
        .doesNotContain(e.messageId());
  }
}
