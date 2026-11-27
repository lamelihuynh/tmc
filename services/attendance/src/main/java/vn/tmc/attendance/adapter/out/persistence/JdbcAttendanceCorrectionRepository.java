package vn.tmc.attendance.adapter.out.persistence;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import vn.tmc.attendance.application.port.out.IAttendanceCorrectionRepository;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.AttendanceCorrection;
import vn.tmc.attendance.domain.Direction;

/** PostgreSQL storage of attendance_corrections (V2). Verified by PersistenceAndBrokerIT. */
public class JdbcAttendanceCorrectionRepository implements IAttendanceCorrectionRepository {
  private static final RowMapper<AttendanceCorrection> MAPPER =
      (rs, n) ->
          new AttendanceCorrection(
              rs.getObject("correction_id", UUID.class),
              rs.getObject("original_attempt_id", UUID.class),
              rs.getInt("revision"),
              rs.getObject("employee_uuid", UUID.class),
              Direction.valueOf(rs.getString("direction")),
              rs.getTimestamp("occurred_at").toInstant(),
              Direction.valueOf(rs.getString("previous_direction")),
              rs.getTimestamp("previous_occurred_at").toInstant(),
              rs.getString("reason"),
              rs.getObject("corrected_by", UUID.class),
              AttemptSource.valueOf(rs.getString("source")),
              rs.getTimestamp("corrected_at").toInstant());
  private final JdbcTemplate jdbc;

  public JdbcAttendanceCorrectionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<AttendanceCorrection> findById(UUID correctionId) {
    return jdbc
        .query("SELECT * FROM attendance_corrections WHERE correction_id = ?", MAPPER, correctionId)
        .stream()
        .findFirst();
  }

  @Override
  public boolean add(AttendanceCorrection c) {
    int inserted =
        jdbc.update(
            "INSERT INTO attendance_corrections (correction_id, original_attempt_id, revision, "
                + "employee_uuid, direction, occurred_at, previous_direction, "
                + "previous_occurred_at, reason, corrected_by, source, corrected_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
            c.correctionId(),
            c.originalAttemptId(),
            c.revision(),
            c.employeeUuid(),
            c.direction().name(),
            Timestamp.from(c.scannedAt()),
            c.previousDirection().name(),
            Timestamp.from(c.previousScannedAt()),
            c.reason(),
            c.correctedBy(),
            c.source().name(),
            Timestamp.from(c.correctedAt()));
    return inserted == 1;
  }

  @Override
  public List<AttendanceCorrection> forAttempt(UUID originalAttemptId) {
    return jdbc.query(
        "SELECT * FROM attendance_corrections WHERE original_attempt_id = ? ORDER BY revision",
        MAPPER,
        originalAttemptId);
  }
}
