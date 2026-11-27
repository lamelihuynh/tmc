package vn.tmc.reporting.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.tmc.reporting.application.port.out.IPresenceRepository;
import vn.tmc.reporting.domain.Presence;
import vn.tmc.reporting.domain.ScanRecord;

/**
 * Presence read model in the reporting database (Flyway V2). Uses the injected JdbcTemplate, so
 * inside IdempotentConsumer it joins the transaction that writes the processed_messages marker: a
 * failed handler leaves neither the row nor the marker, and the redelivery is projected again.
 */
public class JdbcPresenceRepository implements IPresenceRepository {
  /** Same order as InMemoryPresenceRepository.LATEST_FIRST. */
  private static final String LATEST_FIRST =
      "scanned_at DESC, aggregate_version DESC, attempt_id DESC";

  private final JdbcTemplate jdbc;

  public JdbcPresenceRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void upsertScan(ScanRecord scan) {
    jdbc.update(
        """
        INSERT INTO attendance_projection
            (attempt_id, employee_uuid, factory_code, direction, scanned_at, aggregate_version)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (attempt_id) DO UPDATE SET
            employee_uuid = EXCLUDED.employee_uuid,
            factory_code = EXCLUDED.factory_code,
            direction = EXCLUDED.direction,
            scanned_at = EXCLUDED.scanned_at,
            aggregate_version = EXCLUDED.aggregate_version,
            updated_at = now()
        WHERE EXCLUDED.aggregate_version > attendance_projection.aggregate_version
        """,
        scan.attemptId(),
        scan.employeeUuid(),
        scan.factoryCode(),
        scan.direction(),
        Timestamp.from(scan.scannedAt()),
        scan.aggregateVersion());
  }

  @Override
  public Optional<Presence> find(UUID employeeUuid, String factoryCode) {
    return jdbc
        .query(
            "SELECT employee_uuid, factory_code, direction, scanned_at, aggregate_version"
                + " FROM attendance_projection WHERE employee_uuid = ? AND factory_code = ?"
                + " ORDER BY "
                + LATEST_FIRST
                + " LIMIT 1",
            JdbcPresenceRepository::presence,
            employeeUuid,
            factoryCode)
        .stream()
        .findFirst();
  }

  @Override
  public List<Presence> byFactory(String factoryCode) {
    return jdbc.query(
        "SELECT DISTINCT ON (employee_uuid)"
            + " employee_uuid, factory_code, direction, scanned_at, aggregate_version"
            + " FROM attendance_projection WHERE factory_code = ?"
            + " ORDER BY employee_uuid, "
            + LATEST_FIRST,
        JdbcPresenceRepository::presence,
        factoryCode);
  }

  private static Presence presence(ResultSet rs, int row) throws SQLException {
    return new Presence(
        rs.getObject("employee_uuid", UUID.class),
        rs.getString("factory_code"),
        rs.getString("direction"),
        rs.getTimestamp("scanned_at").toInstant(),
        rs.getLong("aggregate_version"));
  }
}
