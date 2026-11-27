package vn.tmc.reporting.adapter.out.persistence;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.tmc.reporting.application.port.out.IFraudAlertStore;
import vn.tmc.reporting.domain.FraudAlert;

/** Fraud alerts in table fraud_alerts (Flyway V3), in the same transaction as the projection. */
public class JdbcFraudAlertStore implements IFraudAlertStore {
  private final JdbcTemplate jdbc;

  public JdbcFraudAlertStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void record(FraudAlert a) {
    jdbc.update(
        "INSERT INTO fraud_alerts"
            + " (attempt_id, rule, employee_uuid, factory_code, other_factory, detail, occurred_at)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (attempt_id, rule) DO NOTHING",
        a.attemptId(),
        a.rule(),
        a.employeeUuid(),
        a.factoryCode(),
        a.otherFactory(),
        a.detail(),
        Timestamp.from(a.at()));
  }

  @Override
  public List<FraudAlert> involving(String factoryCode, int limit) {
    return jdbc.query(
        "SELECT attempt_id, employee_uuid, factory_code, other_factory, rule, detail, occurred_at"
            + " FROM fraud_alerts WHERE factory_code = ? OR other_factory = ?"
            + " ORDER BY occurred_at DESC LIMIT ?",
        (rs, row) ->
            new FraudAlert(
                rs.getObject("attempt_id", UUID.class),
                rs.getObject("employee_uuid", UUID.class),
                rs.getString("factory_code"),
                rs.getString("other_factory"),
                rs.getString("rule"),
                rs.getString("detail"),
                rs.getTimestamp("occurred_at").toInstant()),
        factoryCode,
        factoryCode,
        limit);
  }
}
