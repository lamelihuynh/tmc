package vn.tmc.hrintegration.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
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
import vn.tmc.hrintegration.adapter.out.persistence.JdbcSyncLogRepository;
import vn.tmc.hrintegration.application.usecase.PayrollDeadlineCheck;
import vn.tmc.hrintegration.domain.PayrollLine;
import vn.tmc.hrintegration.domain.SyncRecord;
import vn.tmc.hrintegration.domain.SyncStatus;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.testsupport.TmcContainers;

/**
 * The sync log survives in PostgreSQL with everything a retry needs, and the retry query picks the
 * right rows. Needs Docker: ./gradlew :services:hr-integration:integrationTest
 */
@Tag("integration")
@Testcontainers
class SyncLogPersistenceIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();

  static JdbcSyncLogRepository log;

  @BeforeAll
  static void migrate() {
    DataSource ds =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    Flyway.configure().dataSource(ds).load().migrate();
    log = new JdbcSyncLogRepository(new JdbcTemplate(ds));
  }

  private static final Instant T0 = Instant.parse("2026-11-01T01:30:00Z");

  private static SyncRecord row(SyncStatus status, Instant updatedAt, Instant next) {
    var id = Ids.uuidV7();
    return new SyncRecord(
        id,
        status,
        1,
        null,
        updatedAt,
        5,
        id,
        YearMonth.of(2026, 10),
        new PayrollLine(UUID.randomUUID(), "BD", "ENTRY", T0.minus(Duration.ofDays(3)), null),
        next);
  }

  @Test
  void should_store_and_read_back_every_field() {
    var correctionId = Ids.uuidV7();
    var attempt = Ids.uuidV7();
    var saved =
        new SyncRecord(
            attempt,
            SyncStatus.RETRY_PENDING,
            3,
            "refused",
            T0,
            6,
            correctionId,
            YearMonth.of(2026, 10),
            new PayrollLine(UUID.randomUUID(), "LA", "EXIT", T0.minusSeconds(60), attempt),
            T0.plus(Duration.ofMinutes(4)));

    log.save(saved);
    log.save(saved.with(SyncStatus.IN_PROGRESS, null, T0.plusSeconds(1))); // upsert

    var back = log.find(attempt).orElseThrow();
    assertThat(back.status()).isEqualTo(SyncStatus.IN_PROGRESS);
    assertThat(back.attempts()).isEqualTo(4);
    assertThat(back.batchId()).isEqualTo(correctionId);
    assertThat(back.line()).isEqualTo(saved.line());
    assertThat(back.attendancePeriod()).isEqualTo(YearMonth.of(2026, 10));
    assertThat(back.nextAttemptAt()).isNull();
  }

  @Test
  void should_pick_only_rows_due_for_a_retry() {
    var now = T0.plus(Duration.ofHours(1)).truncatedTo(ChronoUnit.MICROS);
    var unknownDue = row(SyncStatus.UNKNOWN, T0, now.minusSeconds(1));
    var unknownLater = row(SyncStatus.UNKNOWN, T0, now.plusSeconds(60));
    var refusedDue = row(SyncStatus.RETRY_PENDING, T0, null);
    var stuck = row(SyncStatus.IN_PROGRESS, now.minus(Duration.ofMinutes(5)), null);
    var running = row(SyncStatus.IN_PROGRESS, now.minusSeconds(10), null);
    var synced = row(SyncStatus.SYNCED, T0, null);
    for (var r : new SyncRecord[] {unknownDue, unknownLater, refusedDue, stuck, running, synced}) {
      log.save(r);
    }

    var due = log.dueForRetry(now, now.minus(Duration.ofMinutes(2)), 100);

    assertThat(due)
        .extracting(SyncRecord::eventId)
        .contains(unknownDue.eventId(), refusedDue.eventId(), stuck.eventId())
        .doesNotContain(unknownLater.eventId(), running.eventId(), synced.eventId());
  }

  @Test
  void should_let_the_deadline_alert_count_rows_from_the_database() {
    log.save(row(SyncStatus.UNKNOWN, T0, null));
    var check = new PayrollDeadlineCheck(log, () -> T0); // 08:30 on the 1st, Vietnam time

    var alert = check.check().orElseThrow();
    assertThat(alert.period()).isEqualTo(YearMonth.of(2026, 10));
    assertThat(alert.outstanding()).isPositive();
  }
}
