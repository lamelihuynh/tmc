package vn.tmc.userqr.adapter.out.persistence;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import vn.tmc.userqr.application.port.out.IEmployeeDirectory;

/**
 * PostgreSQL employee eligibility directory over table {@code employees} (V3__employees.sql, C-02).
 * Stores eligibility only, never names or other PII (ADR-09). Inactive rows are kept so {@link
 * #changedSince(Instant)} ships departures to the edge.
 */
public class JdbcEmployeeDirectory implements IEmployeeDirectory {
  private static final String COLUMNS = "employee_uuid, status, factories, updated_at";

  private static final RowMapper<Eligibility> MAPPER =
      (rs, n) ->
          new Eligibility(
              rs.getObject("employee_uuid", UUID.class),
              "ACTIVE".equals(rs.getString("status")),
              readFactories(rs.getArray("factories")),
              rs.getTimestamp("updated_at").toInstant());

  private final JdbcTemplate jdbc;

  public JdbcEmployeeDirectory(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<Eligibility> findById(UUID employeeUuid) {
    if (employeeUuid == null) {
      return Optional.empty();
    }
    return jdbc
        .query(
            "SELECT " + COLUMNS + " FROM employees WHERE employee_uuid = ?", MAPPER, employeeUuid)
        .stream()
        .findFirst();
  }

  @Override
  public void save(Eligibility e) {
    var factories = e.factories().stream().sorted().toArray(String[]::new);
    jdbc.update(
        con -> {
          var ps =
              con.prepareStatement(
                  "INSERT INTO employees ("
                      + COLUMNS
                      + ") VALUES (?, ?, ?, ?) "
                      + "ON CONFLICT (employee_uuid) DO UPDATE "
                      + "SET status = EXCLUDED.status, "
                      + "factories = EXCLUDED.factories, "
                      + "updated_at = EXCLUDED.updated_at "
                      + "WHERE EXCLUDED.updated_at >= employees.updated_at");
          ps.setObject(1, e.employeeUuid());
          ps.setString(2, e.active() ? "ACTIVE" : "INACTIVE");
          ps.setArray(3, con.createArrayOf("text", factories));
          ps.setTimestamp(4, Timestamp.from(e.updatedAt()));
          return ps;
        });
  }

  @Override
  public void saveIfAbsent(Eligibility e) {
    var factories = e.factories().stream().sorted().toArray(String[]::new);
    jdbc.update(
        con -> {
          var ps =
              con.prepareStatement(
                  "INSERT INTO employees ("
                      + COLUMNS
                      + ") VALUES (?, ?, ?, ?) "
                      + "ON CONFLICT (employee_uuid) DO NOTHING");
          ps.setObject(1, e.employeeUuid());
          ps.setString(2, e.active() ? "ACTIVE" : "INACTIVE");
          ps.setArray(3, con.createArrayOf("text", factories));
          ps.setTimestamp(4, Timestamp.from(e.updatedAt()));
          return ps;
        });
  }

  @Override
  public List<Eligibility> changedSince(Instant since) {
    return jdbc.query(
        "SELECT "
            + COLUMNS
            + " FROM employees WHERE updated_at > ? ORDER BY updated_at, employee_uuid",
        MAPPER,
        Timestamp.from(since));
  }

  private static Set<String> readFactories(java.sql.Array sqlArray) throws SQLException {
    if (sqlArray == null) {
      return Set.of();
    }
    var raw = (Object[]) sqlArray.getArray();
    if (raw == null || raw.length == 0) {
      return Set.of();
    }
    return Arrays.stream(raw).map(Object::toString).collect(Collectors.toUnmodifiableSet());
  }
}
