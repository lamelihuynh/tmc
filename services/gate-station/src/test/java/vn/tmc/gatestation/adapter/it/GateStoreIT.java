package vn.tmc.gatestation.adapter.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.gatestation.bootstrap.GateStationApplication;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.messaging.AuditEvents;
import vn.tmc.platform.messaging.TmcExchange;
import vn.tmc.platform.messaging.jdbc.JdbcOutboxStore;
import vn.tmc.platform.testsupport.TmcContainers;

/**
 * The "gate-store" profile (ADR-14) against a real PostgreSQL: the service starts, Flyway applies
 * db/gate-store, and the tables accept what A-04 and C-08 will write. Needs Docker.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(classes = GateStationApplication.class)
@ActiveProfiles({"inmem", "gate-store"})
class GateStoreIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();

  @DynamicPropertySource
  static void db(DynamicPropertyRegistry r) {
    r.add("tmc.gate.store.url", POSTGRES::getJdbcUrl);
    r.add("tmc.gate.store.username", POSTGRES::getUsername);
    r.add("tmc.gate.store.password", POSTGRES::getPassword);
  }

  @Autowired JdbcTemplate gateStoreJdbc;
  @Autowired Flyway gateStoreFlyway;
  @Autowired ObjectMapper mapper;

  @Test
  void should_migrate_on_start_and_store_gates_with_valid_modes_only() {
    assertThat(gateStoreFlyway.info().current().getVersion().getVersion()).isEqualTo("1");

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
    var outbox = new JdbcOutboxStore(gateStoreJdbc, mapper);
    var e =
        AuditEvents.recorded(
            TmcExchange.GATE_STATION,
            null,
            "MANUAL_OPEN",
            "BD-GATE-01",
            "FIRE_DRILL",
            UUID.randomUUID(),
            "corr",
            Instant.now());

    outbox.add(e);

    assertThat(outbox.fetchPending(10))
        .extracting(EventEnvelope::messageId)
        .contains(e.messageId());
  }
}
