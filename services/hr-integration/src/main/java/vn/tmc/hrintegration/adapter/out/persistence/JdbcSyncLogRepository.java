package vn.tmc.hrintegration.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.tmc.hrintegration.application.port.out.ISyncLogRepository;
import vn.tmc.hrintegration.domain.PayrollLine;
import vn.tmc.hrintegration.domain.SyncRecord;
import vn.tmc.hrintegration.domain.SyncStatus;

/**
 * Sync log in the hr_integration database (Flyway V2). Uses the injected JdbcTemplate, so inside
 * IdempotentConsumer it joins the transaction of the processed_messages marker; the reconcile job
 * writes outside any transaction, one statement at a time.
 */
public class JdbcSyncLogRepository implements ISyncLogRepository {
  private static final String COLUMNS =
      "event_id, status, attempts, last_error, updated_at, aggregate_version, batch_id,"
          + " attendance_period, employee_uuid, factory_code, direction, scanned_at,"
          + " corrects_event_id, next_attempt_at";

  private final JdbcTemplate jdbc;

  public JdbcSyncLogRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<SyncRecord> find(UUID eventId) {
    return jdbc
        .query(
            "SELECT " + COLUMNS + " FROM sync_log WHERE event_id = ?",
            JdbcSyncLogRepository::record,
            eventId)
        .stream()
        .findFirst();
  }

  @Override
  public void save(SyncRecord r) {
    var line = r.line();
    jdbc.update(
        "INSERT INTO sync_log ("
            + COLUMNS
            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            + " ON CONFLICT (event_id) DO UPDATE SET status = EXCLUDED.status,"
            + " attempts = EXCLUDED.attempts, last_error = EXCLUDED.last_error,"
            + " updated_at = EXCLUDED.updated_at, aggregate_version = EXCLUDED.aggregate_version,"
            + " batch_id = EXCLUDED.batch_id, attendance_period = EXCLUDED.attendance_period,"
            + " employee_uuid = EXCLUDED.employee_uuid, factory_code = EXCLUDED.factory_code,"
            + " direction = EXCLUDED.direction, scanned_at = EXCLUDED.scanned_at,"
            + " corrects_event_id = EXCLUDED.corrects_event_id,"
            + " next_attempt_at = EXCLUDED.next_attempt_at",
        r.eventId(),
        r.status().name(),
        r.attempts(),
        r.lastError(),
        Timestamp.from(r.updatedAt()),
        r.aggregateVersion(),
        r.batchId(),
        r.attendancePeriod() == null ? null : r.attendancePeriod().toString(),
        line == null ? null : line.employeeUuid(),
        line == null ? null : line.factoryCode(),
        line == null ? null : line.direction(),
        line == null ? null : Timestamp.from(line.scannedAt()),
        line == null ? null : line.correctsEventId(),
        r.nextAttemptAt() == null ? null : Timestamp.from(r.nextAttemptAt()));
  }

  @Override
  public List<SyncRecord> byStatus(SyncStatus status) {
    return jdbc.query(
        "SELECT " + COLUMNS + " FROM sync_log WHERE status = ? ORDER BY updated_at",
        JdbcSyncLogRepository::record,
        status.name());
  }

  @Override
  public List<SyncRecord> dueForRetry(Instant now, Instant stuckBefore, int limit) {
    return jdbc.query(
        "SELECT "
            + COLUMNS
            + " FROM sync_log WHERE (status IN ('UNKNOWN', 'RETRY_PENDING')"
            + " AND (next_attempt_at IS NULL OR next_attempt_at <= ?))"
            + " OR (status = 'IN_PROGRESS' AND updated_at < ?)"
            + " ORDER BY updated_at LIMIT ?",
        JdbcSyncLogRepository::record,
        Timestamp.from(now),
        Timestamp.from(stuckBefore),
        limit);
  }

  private static SyncRecord record(ResultSet rs, int row) throws SQLException {
    var employee = rs.getObject("employee_uuid", UUID.class);
    var scannedAt = rs.getTimestamp("scanned_at");
    var line =
        employee == null || scannedAt == null
            ? null
            : new PayrollLine(
                employee,
                rs.getString("factory_code"),
                rs.getString("direction"),
                scannedAt.toInstant(),
                rs.getObject("corrects_event_id", UUID.class));
    var period = rs.getString("attendance_period");
    var next = rs.getTimestamp("next_attempt_at");
    return new SyncRecord(
        rs.getObject("event_id", UUID.class),
        SyncStatus.valueOf(rs.getString("status")),
        rs.getInt("attempts"),
        rs.getString("last_error"),
        rs.getTimestamp("updated_at").toInstant(),
        rs.getLong("aggregate_version"),
        rs.getObject("batch_id", UUID.class),
        period == null ? null : YearMonth.parse(period),
        line,
        next == null ? null : next.toInstant());
  }
}
