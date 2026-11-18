package vn.tmc.attendance.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.AttendanceTransaction;
import vn.tmc.attendance.domain.Direction;

/**
 * PostgreSQL storage of attendance_transactions. The primary key attempt_id plus ON CONFLICT DO
 * NOTHING is what guarantees a replay cannot count twice (DP-02).
 *
 * <p>Verified against PostgreSQL by PersistenceAndBrokerIT.
 */
public class JdbcAttendanceTransactionRepository implements IAttendanceTransactionRepository {
  private final JdbcTemplate jdbc;

  public JdbcAttendanceTransactionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean saveIfAbsent(AttendanceTransaction t) {
    int inserted =
        jdbc.update(
            "INSERT INTO attendance_transactions (attempt_id, employee_uuid, factory_code, "
                + "gate_id, direction, occurred_at, source) VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (attempt_id) DO NOTHING",
            t.attemptId(),
            t.employeeUuid(),
            t.factoryCode(),
            t.gateId(),
            t.direction().name(),
            Timestamp.from(t.occurredAt()),
            t.source().name());
    return inserted == 1;
  }

  @Override
  public Optional<AttendanceTransaction> latestBefore(UUID employeeUuid, Instant at) {
    return jdbc
        .query(
            "SELECT attempt_id, employee_uuid, factory_code, gate_id, direction, occurred_at, "
                + "source FROM attendance_transactions WHERE employee_uuid = ? "
                + "AND occurred_at < ? ORDER BY occurred_at DESC LIMIT 1",
            (rs, n) ->
                new AttendanceTransaction(
                    rs.getObject("attempt_id", UUID.class),
                    rs.getObject("employee_uuid", UUID.class),
                    rs.getString("factory_code"),
                    rs.getString("gate_id"),
                    Direction.valueOf(rs.getString("direction")),
                    rs.getTimestamp("occurred_at").toInstant(),
                    AttemptSource.valueOf(rs.getString("source"))),
            employeeUuid,
            Timestamp.from(at))
        .stream()
        .findFirst();
  }
}
