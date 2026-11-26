package vn.tmc.attendance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import vn.tmc.attendance.adapter.out.persistence.JdbcAttemptRepository;
import vn.tmc.attendance.adapter.out.persistence.JdbcAttendanceCorrectionRepository;
import vn.tmc.attendance.adapter.out.persistence.JdbcAttendanceTransactionRepository;
import vn.tmc.attendance.adapter.out.persistence.JdbcOutboxRepository;
import vn.tmc.attendance.adapter.out.persistence.SpringTransactionRunner;
import vn.tmc.attendance.application.port.in.GateResult;
import vn.tmc.attendance.application.port.in.IAttendanceCommand.RecordAttemptCommand;
import vn.tmc.attendance.application.port.in.IAttendanceCorrection.CorrectAttendanceCommand;
import vn.tmc.attendance.application.port.in.IManualAttendanceVerification.VerifyManualAttendanceCommand;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.application.usecase.AttendanceCommandService;
import vn.tmc.attendance.application.usecase.AttendanceEventFactory;
import vn.tmc.attendance.application.usecase.CorrectAttendanceUseCase;
import vn.tmc.attendance.application.usecase.VerifyManualAttendanceUseCase;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.attendance.domain.AttendanceCorrection;
import vn.tmc.attendance.domain.AttendanceTransaction;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;

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
  static DataSource dataSource;
  static ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private static final Instant HR_TIME = Instant.parse("2026-11-20T12:00:00.123456Z");
  private static final Principal HR =
      new Principal(
          UUID.fromString("0192a1c0-0000-7000-8000-00000000a003"),
          Set.of(Role.HR_STAFF),
          DataScope.all());

  @BeforeAll
  static void migrate() {
    dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    Flyway.configure().dataSource(dataSource).load().migrate();
    jdbc = new JdbcTemplate(dataSource);
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

    assertThat(outbox.fetchPending(1000))
        .extracting(EventEnvelope::messageId)
        .containsSubsequence(first.messageId(), second.messageId());
    outbox.markPublished(first.messageId());

    assertThat(outbox.fetchPending(1000))
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

  @Test
  void should_rollback_a_correction_and_every_outbox_insert_when_staging_fails() {
    for (int failingInsert : new int[] {1, 2}) {
      var f = new HrDatabaseFixture();
      var id =
          f.confirmed(AttemptSource.ONLINE, Ids.uuidV7(), Direction.ENTRY, HR_TIME.minusSeconds(60));
      var original = f.transactions.findByAttemptId(id).orElseThrow();
      var request = correction(id, Direction.EXIT, HR_TIME.minusSeconds(90));
      int before = eventsFor(id).size();
      var failing = f.correction(failAfterInsert(f.outbox, failingInsert));

      assertThatThrownBy(() -> failing.correct(HR, request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("injected outbox failure");

      assertThat(f.corrections.findById(request.correctionId())).isEmpty();
      assertThat(f.corrections.forAttempt(id)).isEmpty();
      assertThat(f.transactions.findByAttemptId(id)).contains(original);
      assertThat(eventsFor(id)).hasSize(before);
      assertThat(eventsFor(id))
          .extracting(EventEnvelope::eventType)
          .doesNotContain("AttendanceCorrected");
    }
  }

  @Test
  void should_persist_a_correction_and_replay_it_with_a_new_use_case_instance() {
    var f = new HrDatabaseFixture();
    var id =
        f.confirmed(AttemptSource.ONLINE, Ids.uuidV7(), Direction.ENTRY, HR_TIME.minusSeconds(60));
    var original = f.transactions.findByAttemptId(id).orElseThrow();
    var request = correction(id, Direction.EXIT, Instant.parse("2026-11-20T11:55:00.123456789Z"));
    int before = eventsFor(id).size();

    var result = f.correction(f.outbox).correct(HR, request);
    var restarted = new HrDatabaseFixture();
    var replay = restarted.correction(restarted.outbox).correct(HR, request);

    assertThat(result.idempotentReplay()).isFalse();
    assertThat(replay.idempotentReplay()).isTrue();
    assertThat(replay.correction()).isEqualTo(result.correction());
    assertThat(replay.aggregateVersion()).isEqualTo(result.aggregateVersion());
    assertThat(restarted.transactions.findByAttemptId(id)).contains(original);
    assertThat(restarted.corrections.forAttempt(id)).containsExactly(result.correction());
    var added = eventsFor(id).subList(before, eventsFor(id).size());
    assertThat(added)
        .extracting(EventEnvelope::eventType)
        .containsExactly("AttendanceCorrected", "AuditRecorded");
    assertThat(added.get(1).payload())
        .containsEntry("actor", HR.subject().toString())
        .containsEntry("reason", request.reason());
  }

  @Test
  void should_apply_the_latest_correction_before_last_pass_ordering_and_cutoff() {
    var f = new HrDatabaseFixture();
    var employee = Ids.uuidV7();
    var earlier =
        f.confirmed(AttemptSource.ONLINE, employee, Direction.ENTRY, HR_TIME.minusSeconds(600));
    var later =
        f.confirmed(AttemptSource.ONLINE, employee, Direction.EXIT, HR_TIME.minusSeconds(60));
    var service = f.correction(f.outbox);
    service.correct(HR, correction(earlier, Direction.EXIT, HR_TIME.minusSeconds(30)));

    assertThat(f.transactions.latestBefore(employee, HR_TIME))
        .hasValueSatisfying(
            t -> {
              assertThat(t.attemptId()).isEqualTo(earlier);
              assertThat(t.direction()).isEqualTo(Direction.EXIT);
              assertThat(t.occurredAt()).isEqualTo(HR_TIME.minusSeconds(30));
            });
    // The corrected timestamp, not the original timestamp or an older revision, decides the cutoff.
    service.correct(HR, correction(earlier, Direction.ENTRY, HR_TIME));
    assertThat(f.transactions.latestBefore(employee, HR_TIME))
        .hasValueSatisfying(t -> assertThat(t.attemptId()).isEqualTo(later));
    assertThat(f.transactions.latestBefore(employee, HR_TIME.plusSeconds(1)))
        .hasValueSatisfying(t -> assertThat(t.attemptId()).isEqualTo(earlier));
    assertThat(f.transactions.findByAttemptId(earlier).orElseThrow().occurredAt())
        .isEqualTo(HR_TIME.minusSeconds(600));
  }

  @Test
  void should_serialize_concurrent_corrections_with_distinct_keys() throws Exception {
    var f = new HrDatabaseFixture();
    var id =
        f.confirmed(AttemptSource.ONLINE, Ids.uuidV7(), Direction.ENTRY, HR_TIME.minusSeconds(60));
    var otherInstance = new HrDatabaseFixture();
    var left = correction(id, Direction.EXIT, HR_TIME.minusSeconds(90));
    var right = correction(id, Direction.ENTRY, HR_TIME.minusSeconds(120));
    int before = eventsFor(id).size();

    var results =
        race(
            () -> f.correction(f.outbox).correct(HR, left),
            () -> otherInstance.correction(otherInstance.outbox).correct(HR, right));

    assertThat(results).allSatisfy(r -> assertThat(r.idempotentReplay()).isFalse());
    var history = f.corrections.forAttempt(id);
    assertThat(history).extracting(AttendanceCorrection::revision).containsExactly(1, 2);
    assertThat(history.get(1).previousDirection()).isEqualTo(history.get(0).direction());
    assertThat(history.get(1).previousScannedAt()).isEqualTo(history.get(0).scannedAt());
    var added = eventsFor(id).subList(before, eventsFor(id).size());
    assertThat(added)
        .extracting(EventEnvelope::eventType)
        .containsExactly(
            "AttendanceCorrected", "AuditRecorded", "AttendanceCorrected", "AuditRecorded");
    assertThat(added.get(2).aggregateVersion()).isEqualTo(added.get(0).aggregateVersion() + 1);
  }

  @Test
  void should_create_one_correction_and_event_pair_for_concurrent_retries() throws Exception {
    var f = new HrDatabaseFixture();
    var id =
        f.confirmed(AttemptSource.ONLINE, Ids.uuidV7(), Direction.ENTRY, HR_TIME.minusSeconds(60));
    var otherInstance = new HrDatabaseFixture();
    var request = correction(id, Direction.EXIT, HR_TIME.minusSeconds(90));
    int before = eventsFor(id).size();

    var results =
        race(
            () -> f.correction(f.outbox).correct(HR, request),
            () -> otherInstance.correction(otherInstance.outbox).correct(HR, request));

    assertThat(results)
        .extracting(r -> r.idempotentReplay())
        .containsExactlyInAnyOrder(false, true);
    assertThat(results.get(0).correction()).isEqualTo(results.get(1).correction());
    assertThat(f.corrections.forAttempt(id)).hasSize(1);
    assertThat(eventsFor(id)).hasSize(before + 2);
  }

  @Test
  void should_rollback_manual_verification_and_every_outbox_insert_when_staging_fails() {
    for (int failingInsert : new int[] {1, 2}) {
      var f = new HrDatabaseFixture();
      var id =
          f.confirmed(AttemptSource.MANUAL, Ids.uuidV7(), Direction.ENTRY, HR_TIME.minusSeconds(60));
      var beforeAttempt = f.attempts.findById(id).orElseThrow();
      int before = eventsFor(id).size();
      var service = f.verification(failAfterInsert(f.outbox, failingInsert));

      assertThatThrownBy(() -> service.verify(HR, verification(id)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("injected outbox failure");

      var loaded = f.attempts.findById(id).orElseThrow();
      assertThat(loaded.version()).isEqualTo(beforeAttempt.version());
      assertThat(loaded.status()).isEqualTo(AttemptStatus.COMMITTED);
      assertThat(loaded.manualVerifiedBy()).isNull();
      assertThat(loaded.manualVerifiedAt()).isNull();
      assertThat(loaded.manualVerificationReason()).isNull();
      assertThat(loaded.countsAsAttendance()).isFalse();
      assertThat(f.transactions.findByAttemptId(id)).isEmpty();
      assertThat(eventsFor(id)).hasSize(before);
      assertThat(eventsFor(id))
          .extracting(EventEnvelope::eventType)
          .doesNotContain("AttendanceConfirmed");
    }
  }

  @Test
  void should_persist_manual_evidence_replay_and_then_allow_a_correction() {
    var f = new HrDatabaseFixture();
    var id =
        f.confirmed(AttemptSource.MANUAL, Ids.uuidV7(), Direction.ENTRY, HR_TIME.minusSeconds(60));
    assertThat(f.transactions.findByAttemptId(id)).isEmpty();
    assertThat(f.attempts.findById(id).orElseThrow().countsAsAttendance()).isFalse();
    int before = eventsFor(id).size();

    var result = f.verification(f.outbox).verify(HR, verification(id));
    var restarted = new HrDatabaseFixture();
    var replay = restarted.verification(restarted.outbox).verify(HR, verification(id));

    assertThat(replay.idempotentReplay()).isTrue();
    assertThat(replay.verifiedBy()).isEqualTo(result.verifiedBy()).isEqualTo(HR.subject());
    assertThat(replay.verifiedAt()).isEqualTo(result.verifiedAt()).isEqualTo(HR_TIME);
    assertThat(replay.reason()).isEqualTo(result.reason());
    assertThat(replay.aggregateVersion()).isEqualTo(result.aggregateVersion());
    assertThat(restarted.attempts.findById(id).orElseThrow().countsAsAttendance()).isTrue();
    assertThat(restarted.transactions.findByAttemptId(id)).isPresent();
    var added = eventsFor(id).subList(before, eventsFor(id).size());
    assertThat(added)
        .extracting(EventEnvelope::eventType)
        .containsExactly("AttendanceConfirmed", "AuditRecorded");
    assertThat(added.get(0).payload()).containsEntry("source", "MANUAL");
    assertThat(added.get(1).payload()).containsEntry("actor", HR.subject().toString());

    var corrected =
        restarted
            .correction(restarted.outbox)
            .correct(HR, correction(id, Direction.EXIT, HR_TIME.minusSeconds(90)));
    assertThat(corrected.aggregateVersion()).isEqualTo(result.aggregateVersion() + 1);
    assertThat(corrected.correction().source()).isEqualTo(AttemptSource.MANUAL);
  }

  @Test
  void should_count_one_manual_pass_under_concurrent_verification() throws Exception {
    var f = new HrDatabaseFixture();
    var id =
        f.confirmed(AttemptSource.MANUAL, Ids.uuidV7(), Direction.ENTRY, HR_TIME.minusSeconds(60));
    var otherInstance = new HrDatabaseFixture();
    int before = eventsFor(id).size();

    var results =
        race(
            () -> f.verification(f.outbox).verify(HR, verification(id)),
            () -> otherInstance.verification(otherInstance.outbox).verify(HR, verification(id)));

    assertThat(results)
        .extracting(r -> r.idempotentReplay())
        .containsExactlyInAnyOrder(false, true);
    assertThat(results.get(0).aggregateVersion()).isEqualTo(results.get(1).aggregateVersion());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM attendance_transactions WHERE attempt_id = ?",
                Integer.class,
                id))
        .isEqualTo(1);
    assertThat(eventsFor(id)).hasSize(before + 2);
    assertThat(eventsFor(id).stream().filter(e -> e.eventType().equals("AttendanceConfirmed")))
        .hasSize(1);
    assertThat(f.attempts.findById(id).orElseThrow().manualVerifiedBy()).isEqualTo(HR.subject());
  }

  @Test
  void should_reject_stale_or_fabricated_newer_snapshots_without_erasing_verification() {
    var f = new HrDatabaseFixture();
    var id =
        f.confirmed(AttemptSource.MANUAL, Ids.uuidV7(), Direction.ENTRY, HR_TIME.minusSeconds(60));
    var stale = f.attempts.findById(id).orElseThrow();
    var result = f.verification(f.outbox).verify(HR, verification(id));
    var withoutEvidenceAtHigherVersion =
        AccessAttempt.restore(
            stale.attemptId(),
            stale.employeeUuid(),
            stale.gateId(),
            stale.factoryCode(),
            stale.direction(),
            stale.jti(),
            stale.source(),
            stale.scannedAt(),
            stale.status(),
            stale.reasonCode(),
            stale.anomaly(),
            HR_TIME,
            result.aggregateVersion() + 1);
    int before = eventsFor(id).size();

    for (var invalid : List.of(stale, withoutEvidenceAtHigherVersion)) {
      assertThatThrownBy(
              () ->
                  f.tx.inTransaction(
                      () -> {
                        f.outbox.add(f.events.auditRecorded(invalid, "STALE_WRITE_TEST", "it-stale"));
                        f.attempts.save(invalid);
                        return null;
                      }))
          .isInstanceOf(IllegalStateException.class);
    }

    var loaded = f.attempts.findById(id).orElseThrow();
    assertThat(loaded.manualVerifiedBy()).isEqualTo(result.verifiedBy());
    assertThat(loaded.manualVerifiedAt()).isEqualTo(result.verifiedAt());
    assertThat(loaded.manualVerificationReason()).isEqualTo(result.reason());
    assertThat(loaded.version()).isEqualTo(result.aggregateVersion());
    assertThat(loaded.countsAsAttendance()).isTrue();
    assertThat(eventsFor(id)).hasSize(before);
  }

  private static CorrectAttendanceCommand correction(UUID id, Direction direction, Instant at) {
    return new CorrectAttendanceCommand(
        Ids.uuidV7(), id, direction, at, "WRONG_DIRECTION", "it-hr");
  }

  private static VerifyManualAttendanceCommand verification(UUID id) {
    return new VerifyManualAttendanceCommand(id, "SUPERVISOR_CONFIRMED", "it-hr");
  }

  private static List<EventEnvelope> eventsFor(UUID attemptId) {
    return jdbc.query(
        "SELECT envelope::text FROM outbox_events WHERE envelope->>'eventId' = ? ORDER BY seq",
        (rs, index) -> {
          try {
            return mapper.readValue(rs.getString(1), EventEnvelope.class);
          } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("invalid stored test event", e);
          }
        },
        attemptId.toString());
  }

  private static IOutboxRepository failAfterInsert(IOutboxRepository actual, int failingInsert) {
    var inserted = new AtomicInteger();
    return new IOutboxRepository() {
      @Override
      public void add(EventEnvelope envelope) {
        actual.add(envelope);
        if (inserted.incrementAndGet() == failingInsert) {
          throw new IllegalStateException("injected outbox failure");
        }
      }

      @Override
      public List<EventEnvelope> fetchPending(int limit) {
        return actual.fetchPending(limit);
      }

      @Override
      public void markPublished(UUID messageId) {
        actual.markPublished(messageId);
      }

      @Override
      public void recordFailure(UUID messageId) {
        actual.recordFailure(messageId);
      }
    };
  }

  private static <T> List<T> race(Callable<T> left, Callable<T> right) throws Exception {
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var workers = Executors.newVirtualThreadPerTaskExecutor();
    try {
      var a =
          workers.submit(
              () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                  throw new IllegalStateException("start timeout");
                }
                return left.call();
              });
      var b =
          workers.submit(
              () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                  throw new IllegalStateException("start timeout");
                }
                return right.call();
              });
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
    } finally {
      start.countDown();
      workers.shutdownNow();
    }
  }

  /** Independent instances share only the actual database, never an application mutex. */
  private static class HrDatabaseFixture {
    final JdbcAttemptRepository attempts = new JdbcAttemptRepository(jdbc);
    final JdbcAttendanceTransactionRepository transactions =
        new JdbcAttendanceTransactionRepository(jdbc);
    final JdbcAttendanceCorrectionRepository corrections =
        new JdbcAttendanceCorrectionRepository(jdbc);
    final JdbcOutboxRepository outbox = new JdbcOutboxRepository(jdbc, mapper);
    final SpringTransactionRunner tx;
    final AttendanceEventFactory events = new AttendanceEventFactory(() -> HR_TIME);
    final AttendanceCommandService commands;

    HrDatabaseFixture() {
      var template = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
      template.setTimeout(10);
      tx = new SpringTransactionRunner(template);
      commands =
          new AttendanceCommandService(attempts, transactions, outbox, tx, events, () -> HR_TIME);
    }

    CorrectAttendanceUseCase correction(IOutboxRepository targetOutbox) {
      return new CorrectAttendanceUseCase(
          attempts, transactions, corrections, targetOutbox, tx, events, () -> HR_TIME);
    }

    VerifyManualAttendanceUseCase verification(IOutboxRepository targetOutbox) {
      return new VerifyManualAttendanceUseCase(
          attempts, transactions, targetOutbox, tx, events, () -> HR_TIME);
    }

    UUID confirmed(AttemptSource source, UUID employee, Direction direction, Instant at) {
      var id = Ids.uuidV7();
      commands.recordAttempt(
          new RecordAttemptCommand(
              id, employee, "BD-GATE-01", "BD", direction, "it-" + id, source, at, "it-setup"));
      commands.markGateCommanded(id);
      commands.recordGateResult(id, GateResult.OPENED, "it-setup");
      return id;
    }
  }

  private EventEnvelope envelope(String type) {
    return new EventEnvelope(
        Ids.uuidV7(), type, 1, Ids.uuidV7(), 1, Instant.now(), "corr", "it", "BD", "G1", null);
  }
}
