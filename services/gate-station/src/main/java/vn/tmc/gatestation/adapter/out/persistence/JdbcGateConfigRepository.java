package vn.tmc.gatestation.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import vn.tmc.gatestation.application.port.out.IGateConfigRepository;
import vn.tmc.gatestation.domain.GateConfig;
import vn.tmc.gatestation.domain.GateMode;

/** Gate catalog on the Gate Store table {@code gates} (FR-11 to FR-16, ADR-14). */
public class JdbcGateConfigRepository implements IGateConfigRepository {
  private static final RowMapper<GateConfig> MAPPER =
      (rs, row) ->
          new GateConfig(
              rs.getString("gate_id"),
              rs.getString("factory_code"),
              rs.getString("hardware_gate_id"),
              GateMode.valueOf(rs.getString("mode")),
              rs.getBoolean("maintenance"));

  private final JdbcTemplate jdbc;

  public JdbcGateConfigRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<GateConfig> find(String gateId) {
    return jdbc
        .query(
            "SELECT gate_id, factory_code, hardware_gate_id, mode, maintenance "
                + "FROM gates WHERE gate_id = ?",
            MAPPER,
            gateId)
        .stream()
        .findFirst();
  }

  @Override
  public List<GateConfig> all() {
    return jdbc.query(
        "SELECT gate_id, factory_code, hardware_gate_id, mode, maintenance "
            + "FROM gates ORDER BY gate_id",
        MAPPER);
  }

  @Override
  public GateConfig upsert(GateConfig gate) {
    jdbc.update(
        "INSERT INTO gates (gate_id, factory_code, hardware_gate_id, mode, maintenance, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, now()) "
            + "ON CONFLICT (gate_id) DO UPDATE SET "
            + "factory_code = EXCLUDED.factory_code, "
            + "hardware_gate_id = EXCLUDED.hardware_gate_id, "
            + "mode = EXCLUDED.mode, "
            + "maintenance = EXCLUDED.maintenance, "
            + "updated_at = now()",
        gate.gateId(),
        gate.factoryCode(),
        gate.hardwareGateId(),
        gate.mode().name(),
        gate.maintenance());
    return gate;
  }
}
