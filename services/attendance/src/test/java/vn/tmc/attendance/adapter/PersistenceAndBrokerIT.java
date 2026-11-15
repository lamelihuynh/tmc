package vn.tmc.attendance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import vn.tmc.attendance.adapter.out.persistence.JdbcAttendanceCorrectionRepository;
import vn.tmc.attendance.adapter.out.persistence.JdbcAttendanceTransactionRepository;
import vn.tmc.attendance.adapter.out.persistence.JdbcOutboxRepository;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.attendance.domain.AttendanceCorrection;
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

  @Test
  void should_find_attempts_by_status_and_age_and_detect_a_reused_jti() {
    var repo = new JdbcAttemptRepository(jdbc);
    var a = attempt(Ids.uuidV7(), "jti-it-status");
    repo.save(a);
    a.reserve(Instant.now());
    repo.save(a);

    assertThat(repo.findByStatus(AttemptStatus.RESERVED, 1000))
        .extracting(AccessAttempt::attemptId)
        .contains(a.attemptId());
    assertThat(
            repo.findByStatusUpdatedBefore(
                AttemptStatus.RESERVED, Instant.now().plusSeconds(60), 1000))
        .extracting(AccessAttempt::attemptId)
        .contains(a.attemptId());
    assertThat(
            repo.findByStatusUpdatedBefore(
                AttemptStatus.RESERVED, Instant.now().minusSeconds(3600), 1000))
        .extracting(AccessAttempt::attemptId)
        .doesNotContain(a.attemptId());
    assertThat(repo.existsOtherWithJti("jti-it-status", a.attemptId())).isFalse();
    assertThat(repo.existsOtherWithJti("jti-it-status", Ids.uuidV7())).isTrue();
  }

  @Test
  void should_answer_the_latest_counted_pass_before_a_time() {
    var attempts = new JdbcAttemptRepository(jdbc);
    var tx = new JdbcAttendanceTransactionRepository(jdbc);
    var employee = UUID.randomUUID();
    var morning = Instant.parse("2026-11-20T07:00:00Z");
    var evening = Instant.parse("2026-11-20T17:00:00Z");
    for (var pass : new Object[][] {{morning, Direction.ENTRY}, {evening, Direction.EXIT}}) {
      var a =
          AccessAttempt.validated(
              Ids.uuidV7(),
              employee,
              "BD-GATE-03",
              "BD",
              (Direction) pass[1],
              null,
              AttemptSource.ONLINE,
              (Instant) pass[0],
              (Instant) pass[0]);
      attempts.save(a);
      tx.saveIfAbsent(
          new AttendanceTransaction(
              a.attemptId(),
              employee,
              "BD",
              "BD-GATE-03",
              (Direction) pass[1],
              (Instant) pass[0],
              AttemptSource.ONLINE));
    }

    assertThat(tx.lastDirectionBefore(employee, Instant.parse("2026-11-20T12:00:00Z")))
        .contains(Direction.ENTRY);
    assertThat(tx.lastDirectionBefore(employee, Instant.parse("2026-11-21T00:00:00Z")))
        .contains(Direction.EXIT);
    assertThat(tx.latestBefore(employee, morning)).isEmpty();
  }

  @Test
  void should_count_publish_failures_without_publishing_the_row() {
    var outbox = new JdbcOutboxRepository(jdbc, mapper);
    var e = envelope("C");
    outbox.add(e);

    outbox.recordFailure(e.messageId());
    outbox.recordFailure(e.messageId());

    assertThat(
            jdbc.queryForObject(
                "SELECT failure_count FROM outbox_events WHERE message_id = ?",
                Integer.class,
                e.messageId()))
        .isEqualTo(2);
    assertThat(outbox.fetchPending(1000))
        .extracting(EventEnvelope::messageId)
        .contains(e.messageId());
  }

  @Test
  void should_store_corrections_as_revisions_linked_to_the_original_transaction() {
    var attempts = new JdbcAttemptRepository(jdbc);
    var tx = new JdbcAttendanceTransactionRepository(jdbc);
    var corrections = new JdbcAttendanceCorrectionRepository(jdbc);
    var a = attempt(Ids.uuidV7(), "jti-it-3");
    attempts.save(a);
    var original =
        new AttendanceTransaction(
            a.attemptId(),
            a.employeeUuid(),
            "BD",
            "BD-GATE-01",
            Direction.ENTRY,
            Instant.parse("2026-11-20T07:20:00Z"),
            AttemptSource.ONLINE);
    tx.saveIfAbsent(original);
    var hr = UUID.randomUUID();
    var first =
        AttendanceCorrection.first(
            Ids.uuidV7(),
            original,
            Direction.ENTRY,
            Instant.parse("2026-11-20T06:55:00Z"),
            "MISSED_SCAN_CONFIRMED_BY_SUPERVISOR",
            hr,
            Instant.now());
    var clash =
        AttendanceCorrection.first(
            Ids.uuidV7(),
            original,
            Direction.EXIT,
            Instant.now(),
            "WRONG_DIRECTION",
            hr,
            Instant.now());

    assertThat(corrections.add(first)).isTrue();
    assertThat(corrections.add(clash)).isFalse(); // same revision: unique constraint

    var loaded = corrections.latest(original.attemptId()).orElseThrow();
    assertThat(loaded.scannedAt()).isEqualTo(first.scannedAt());
    assertThat(loaded.previousScannedAt()).isEqualTo(original.occurredAt());
    // the original row is untouched: still exactly one transaction for the attempt
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM attendance_transactions WHERE attempt_id = ?",
                Integer.class,
                original.attemptId()))
        .isEqualTo(1);
  }

  @Test
  void should_not_accept_a_correction_of_a_transaction_that_does_not_exist() {
    var ghost =
        new AttendanceTransaction(
            Ids.uuidV7(),
            UUID.randomUUID(),
            "BD",
            "BD-GATE-01",
            Direction.ENTRY,
            Instant.now(),
            AttemptSource.ONLINE);
    var c =
        AttendanceCorrection.first(
            Ids.uuidV7(),
            ghost,
            Direction.EXIT,
            Instant.now(),
            "WRONG_DIRECTION",
            UUID.randomUUID(),
            Instant.now());

    assertThatThrownBy(() -> new JdbcAttendanceCorrectionRepository(jdbc).add(c))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  private EventEnvelope envelope(String type) {
    return new EventEnvelope(
        Ids.uuidV7(), type, 1, Ids.uuidV7(), 1, Instant.now(), "corr", "it", "BD", "G1", null);
  }
}
