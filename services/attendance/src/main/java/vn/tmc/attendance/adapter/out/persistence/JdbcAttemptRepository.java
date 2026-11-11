package vn.tmc.attendance.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.Anomaly;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * PostgreSQL storage of access_attempts (V1__attendance_schema.sql). Save is an upsert on the
 * primary key attempt_id.
 *
 * <p>NOT yet run against a real database. TODO Team B (B-01): run PersistenceAndBrokerIT with
 * Docker.
 */
public class JdbcAttemptRepository implements IAttemptRepository {
  private static final String COLUMNS =
      "attempt_id, employee_uuid, gate_id, factory_code, direction, jti, source, scanned_at, "
          + "status, reason_code, anomaly, updated_at, version";

  private static final RowMapper<AccessAttempt> MAPPER =
      (rs, n) ->
          AccessAttempt.restore(
              rs.getObject("attempt_id", UUID.class),
              rs.getObject("employee_uuid", UUID.class),
              rs.getString("gate_id"),
              rs.getString("factory_code"),
              enumOrNull(Direction.class, rs.getString("direction")),
              rs.getString("jti"),
              AttemptSource.valueOf(rs.getString("source")),
              rs.getTimestamp("scanned_at").toInstant(),
              AttemptStatus.valueOf(rs.getString("status")),
              enumOrNull(ReasonCode.class, rs.getString("reason_code")),
              enumOrNull(Anomaly.class, rs.getString("anomaly")),
              rs.getTimestamp("updated_at").toInstant(),
              rs.getLong("version"));

  private final JdbcTemplate jdbc;

  public JdbcAttemptRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<AccessAttempt> findById(UUID attemptId) {
    return jdbc
        .query(
            "SELECT " + COLUMNS + " FROM access_attempts WHERE attempt_id = ?", MAPPER, attemptId)
        .stream()
        .findFirst();
  }

  @Override
  public void save(AccessAttempt a) {
    jdbc.update(
        "INSERT INTO access_attempts ("
            + COLUMNS
            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT (attempt_id) DO UPDATE SET status = EXCLUDED.status, "
            + "reason_code = EXCLUDED.reason_code, anomaly = EXCLUDED.anomaly, "
            + "updated_at = EXCLUDED.updated_at, version = EXCLUDED.version",
        a.attemptId(),
        a.employeeUuid(),
        a.gateId(),
        a.factoryCode(),
        nameOrNull(a.direction()),
        a.jti(),
        a.source().name(),
        Timestamp.from(a.scannedAt()),
        a.status().name(),
        nameOrNull(a.reasonCode()),
        nameOrNull(a.anomaly()),
        Timestamp.from(a.updatedAt()),
        a.version());
  }

  @Override
  public boolean existsOtherWithJti(String jti, UUID exceptAttemptId) {
    Boolean exists =
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM access_attempts WHERE jti = ? AND attempt_id <> ?)",
            Boolean.class,
            jti,
            exceptAttemptId);
    return Boolean.TRUE.equals(exists);
  }

  @Override
  public List<AccessAttempt> findByStatus(AttemptStatus status, int limit) {
    return jdbc.query(
        "SELECT " + COLUMNS + " FROM access_attempts WHERE status = ? ORDER BY updated_at LIMIT ?",
        MAPPER,
        status.name(),
        limit);
  }

  @Override
  public List<AccessAttempt> findByStatusUpdatedBefore(
      AttemptStatus status, Instant cutoff, int limit) {
    return jdbc.query(
        "SELECT "
            + COLUMNS
            + " FROM access_attempts WHERE status = ? AND updated_at < ? "
            + "ORDER BY updated_at LIMIT ?",
        MAPPER,
        status.name(),
        Timestamp.from(cutoff),
        limit);
  }

  private static String nameOrNull(Enum<?> value) {
    return value == null ? null : value.name();
  }

  private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
    return value == null ? null : Enum.valueOf(type, value);
  }
}
