package vn.tmc.auditadmin.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.auditadmin.adapter.out.persistence.JdbcAuditStore;
import vn.tmc.auditadmin.domain.AuditEntry;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.testsupport.TmcContainers;

/**
 * D-08 against real PostgreSQL. The service login (auditadmin_app, created here as
 * docker/init/central does) can only INSERT and SELECT; the trigger stops even the owner from
 * rewriting history; concurrent writers keep one valid chain; tampering is detected. Needs Docker:
 * ./gradlew :services:audit-admin:integrationTest
 */
@Tag("integration")
@Testcontainers
class AuditStoreIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();

  static JdbcTemplate owner;
  static JdbcTemplate app;
  static JdbcAuditStore store;

  @BeforeAll
  static void migrate() {
    DataSource ownerDs =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    owner = new JdbcTemplate(ownerDs);
    owner.execute("CREATE ROLE auditadmin_app LOGIN PASSWORD 'auditadmin_app'");
    Flyway.configure().dataSource(ownerDs).load().migrate();

    DataSource appDs =
        new DriverManagerDataSource(POSTGRES.getJdbcUrl(), "auditadmin_app", "auditadmin_app");
    app = new JdbcTemplate(appDs);
    store =
        new JdbcAuditStore(
            app,
            new TransactionTemplate(new DataSourceTransactionManager(appDs)),
            new ObjectMapper());
  }

  private static AuditEntry entry(String action) {
    return new AuditEntry(
        Ids.uuidV7(),
        Instant.now(),
        "identity",
        action,
        UUID.randomUUID().toString(),
        "t",
        Map.of("reasonCode", "X", "action", action));
  }

  @Test
  void should_let_the_service_login_insert_and_read_but_never_change_history() {
    var e = entry("LOGIN_FAILED");
    store.append(e);
    assertThat(store.search(null, null, "LOGIN_FAILED", e.actor(), 10))
        .singleElement()
        .isEqualTo(e);

    assertThatThrownBy(
            () ->
                app.update(
                    "UPDATE audit_entries SET action = 'LOGIN_SUCCEEDED' WHERE entry_id = ?",
                    e.entryId()))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("permission denied");
    assertThatThrownBy(
            () -> app.update("DELETE FROM audit_entries WHERE entry_id = ?", e.entryId()))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("permission denied");
    assertThatThrownBy(() -> app.execute("TRUNCATE audit_entries"))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("permission denied");
  }

  @Test
  void should_stop_even_the_owner_from_updating_or_deleting() {
    var e = entry("DEVICE_REVOKED");
    store.append(e);

    assertThatThrownBy(
            () ->
                owner.update(
                    "UPDATE audit_entries SET actor = 'someone-else' WHERE entry_id = ?",
                    e.entryId()))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("append-only");
    assertThatThrownBy(
            () -> owner.update("DELETE FROM audit_entries WHERE entry_id = ?", e.entryId()))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("append-only");
  }

  @Test
  void should_keep_one_valid_chain_under_concurrent_writers_and_ignore_duplicates()
      throws Exception {
    var before = store.count();
    var tasks = new ArrayList<Callable<Void>>();
    for (int i = 0; i < 20; i++) {
      var e = entry("MANUAL_OPEN_REQUESTED");
      tasks.add(
          () -> {
            store.append(e);
            store.append(e); // redelivery
            return null;
          });
    }
    try (var pool = Executors.newFixedThreadPool(8)) {
      for (var f : pool.invokeAll(tasks)) {
        f.get();
      }
    }

    assertThat(store.count()).isEqualTo(before + 20);
    var verification = store.verifyChain();
    assertThat(verification.valid()).isTrue();
    assertThat(verification.checked()).isEqualTo(store.count());
  }

  @Test
  void should_detect_an_entry_rewritten_behind_the_service_back() {
    var e = entry("ROLE_CHANGED");
    store.append(e);
    store.append(entry("LOGIN_SUCCEEDED"));
    assertThat(store.verifyChain().valid()).isTrue();

    // a database superuser switches the trigger off and rewrites one row
    owner.execute("ALTER TABLE audit_entries DISABLE TRIGGER audit_entries_no_update_delete");
    try {
      owner.update(
          "UPDATE audit_entries SET action = 'NOTHING_HAPPENED' WHERE entry_id = ?", e.entryId());
    } finally {
      owner.execute("ALTER TABLE audit_entries ENABLE TRIGGER audit_entries_no_update_delete");
    }

    var verification = store.verifyChain();
    assertThat(verification.valid()).isFalse();
    var seq =
        owner.queryForObject(
            "SELECT seq FROM audit_entries WHERE entry_id = ?", Long.class, e.entryId());
    assertThat(verification.brokenAt()).isEqualTo(seq);

    // put it back so the other tests see a valid chain again
    owner.execute("ALTER TABLE audit_entries DISABLE TRIGGER audit_entries_no_update_delete");
    try {
      owner.update(
          "UPDATE audit_entries SET action = 'ROLE_CHANGED' WHERE entry_id = ?", e.entryId());
    } finally {
      owner.execute("ALTER TABLE audit_entries ENABLE TRIGGER audit_entries_no_update_delete");
    }
    assertThat(store.verifyChain().valid()).isTrue();
  }
}
