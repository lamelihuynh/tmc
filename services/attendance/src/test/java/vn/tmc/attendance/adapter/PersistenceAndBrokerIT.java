package vn.tmc.attendance.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.attendance.adapter.out.persistence.JdbcAttemptRepository;
import vn.tmc.attendance.adapter.out.persistence.JdbcAttendanceTransactionRepository;
import vn.tmc.attendance.adapter.out.persistence.JdbcOutboxRepository;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.attendance.domain.AttendanceTransaction;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.Ids;

/**
 * Integration test against a real PostgreSQL (Flyway migration, upserts, unique constraints, outbox
 * order). Needs Docker. Run with: ./gradlew :services:attendance:integrationTest
 */
@Tag("integration")
@Testcontainers
class PersistenceAndBrokerIT {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  static JdbcTemplate jdbc;
  static ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeAll
  static void migrate() {
    DataSource ds =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    Flyway.configure().dataSource(ds).load().migrate();
    jdbc = new JdbcTemplate(ds);
  }

  private AccessAttempt attempt(UUID id, String jti) {
    var now = Instant.now();
    return AccessAttempt.validated(
        id,
        UUID.randomUUID(),
        "BD-GATE-01",
        "BD",
        Direction.ENTRY,
        jti,
        AttemptSource.ONLINE,
        now,
        now);
  }

  @Test
  void should_upsert_an_attempt_and_read_back_the_new_state() {
    var repo = new JdbcAttemptRepository(jdbc);
    var a = attempt(Ids.uuidV7(), "jti-it-1");
    repo.save(a);
    a.reserve(Instant.now());
    a.commandGate(Instant.now());
    repo.save(a);

    var loaded = repo.findById(a.attemptId()).orElseThrow();

    assertThat(loaded.status()).isEqualTo(AttemptStatus.GATE_COMMANDED);
    assertThat(loaded.version()).isEqualTo(a.version());
  }

  @Test
  void should_not_create_a_second_attendance_transaction_for_the_same_attempt() {
    var attempts = new JdbcAttemptRepository(jdbc);
    var tx = new JdbcAttendanceTransactionRepository(jdbc);
    var a = attempt(Ids.uuidV7(), "jti-it-2");
    attempts.save(a);
    var record =
        new AttendanceTransaction(
            a.attemptId(),
            a.employeeUuid(),
            "BD",
            "BD-GATE-01",
            Direction.ENTRY,
            Instant.now(),
            AttemptSource.ONLINE);

    assertThat(tx.saveIfAbsent(record)).isTrue();
    assertThat(tx.saveIfAbsent(record)).isFalse();
  }

  @Test
  void should_return_pending_outbox_rows_in_insertion_order_and_hide_published_ones() {
    var outbox = new JdbcOutboxRepository(jdbc, mapper);
    var first = envelope("A");
    var second = envelope("B");
    outbox.add(first);
    outbox.add(second);

    assertThat(outbox.fetchPending(10))
        .extracting(EventEnvelope::messageId)
        .contains(first.messageId(), second.messageId());
    outbox.markPublished(first.messageId());

    assertThat(outbox.fetchPending(10))
        .extracting(EventEnvelope::messageId)
        .doesNotContain(first.messageId())
        .contains(second.messageId());
  }

  private EventEnvelope envelope(String type) {
    return new EventEnvelope(
        Ids.uuidV7(), type, 1, Ids.uuidV7(), 1, Instant.now(), "corr", "it", "BD", "G1", null);
  }
}
