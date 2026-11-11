package vn.tmc.userqr.adapter.out.persistence;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import vn.tmc.userqr.application.port.out.IDeviceKeyRegistry;
import vn.tmc.userqr.domain.Device;
import vn.tmc.userqr.domain.DeviceStatus;

/**
 * PostgreSQL registry over table {@code devices} (V1__device_registry.sql). "One ACTIVE device per
 * employee" is also enforced by the partial unique index, so two parallel registrations cannot both
 * win.
 *
 * <p>NOT yet run against a real database. TODO Team C (C-01): integration test with Testcontainers.
 */
public class JdbcDeviceKeyRegistry implements IDeviceKeyRegistry {
  private static final String COLUMNS =
      "device_id, employee_uuid, public_key, status, registered_at, updated_at";

  private static final RowMapper<Device> MAPPER =
      (rs, n) ->
          new Device(
              rs.getString("device_id"),
              rs.getObject("employee_uuid", UUID.class),
              decode(rs.getString("public_key")),
              DeviceStatus.valueOf(rs.getString("status")),
              rs.getTimestamp("registered_at").toInstant(),
              rs.getTimestamp("updated_at").toInstant());

  private final JdbcTemplate jdbc;

  public JdbcDeviceKeyRegistry(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<Device> findById(String deviceId) {
    return jdbc
        .query("SELECT " + COLUMNS + " FROM devices WHERE device_id = ?", MAPPER, deviceId)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<Device> findActiveByEmployee(UUID employeeUuid) {
    return jdbc
        .query(
            "SELECT " + COLUMNS + " FROM devices WHERE employee_uuid = ? AND status = 'ACTIVE'",
            MAPPER,
            employeeUuid)
        .stream()
        .findFirst();
  }

  @Override
  public void save(Device d) {
    jdbc.update(
        "INSERT INTO devices ("
            + COLUMNS
            + ") VALUES (?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT (device_id) DO UPDATE SET status = EXCLUDED.status, "
            + "updated_at = EXCLUDED.updated_at",
        d.deviceId(),
        d.employeeUuid(),
        Base64.getEncoder().encodeToString(d.publicKey().getEncoded()),
        d.status().name(),
        Timestamp.from(d.registeredAt()),
        Timestamp.from(d.updatedAt()));
  }

  @Override
  public List<Device> changedSince(Instant since) {
    return jdbc.query(
        "SELECT " + COLUMNS + " FROM devices WHERE updated_at > ? ORDER BY updated_at",
        MAPPER,
        Timestamp.from(since));
  }

  private static PublicKey decode(String base64) {
    try {
      return KeyFactory.getInstance("EC")
          .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
    } catch (Exception e) {
      throw new IllegalStateException("stored public key is not a valid EC key", e);
    }
  }
}
