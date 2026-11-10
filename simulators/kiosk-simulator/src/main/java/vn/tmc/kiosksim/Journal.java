package vn.tmc.kiosksim;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Kiosk Journal (ADR-11 section 2): an append-only log of EVERY scan, written BEFORE any
 * network call, plus the small caches needed to verify offline (device keys, gates). Raw QR tokens
 * are never stored, only their SHA-256 hash (ADR-09).
 */
public class Journal implements AutoCloseable {

  public record Row(
      long seq,
      UUID attemptId,
      String gateId,
      String tokenHash,
      Instant scannedAt,
      String status,
      String employeeUuid,
      String jti,
      String direction,
      boolean handedOver,
      String result) {}

  public record DeviceKey(
      String deviceId, String employeeUuid, String publicKeyBase64, boolean revoked) {}

  public record Gate(String gateId, String mode) {}

  private final Connection db;

  public Journal(String sqlitePath) {
    try {
      db = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
      try (var st = db.createStatement()) {
        st.execute(
            "CREATE TABLE IF NOT EXISTS journal (journal_seq INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " attempt_id TEXT NOT NULL UNIQUE, gate_id TEXT NOT NULL, token_hash TEXT NOT NULL,"
                + " scanned_at TEXT NOT NULL, status TEXT NOT NULL, employee_uuid TEXT, jti TEXT,"
                + " direction TEXT, handed_over INTEGER NOT NULL DEFAULT 0, result TEXT)");
        st.execute(
            "CREATE TABLE IF NOT EXISTS device_keys (device_id TEXT PRIMARY KEY, employee_uuid TEXT NOT NULL,"
                + " public_key TEXT NOT NULL, revoked INTEGER NOT NULL)");
        st.execute(
            "CREATE TABLE IF NOT EXISTS gates (gate_id TEXT PRIMARY KEY, mode TEXT NOT NULL)");
        st.execute("CREATE TABLE IF NOT EXISTS meta (k TEXT PRIMARY KEY, v TEXT NOT NULL)");
      }
    } catch (SQLException e) {
      throw new IllegalStateException("cannot open journal " + sqlitePath, e);
    }
  }

  // ---------- journal ----------

  /** Write-ahead: durable before the network is touched. */
  public synchronized long append(
      UUID attemptId, String gateId, String tokenHash, Instant scannedAt) {
    try (var ps =
        db.prepareStatement(
            "INSERT INTO journal (attempt_id, gate_id, token_hash, scanned_at, status) VALUES (?,?,?,?, 'RECEIVED')",
            java.sql.Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, attemptId.toString());
      ps.setString(2, gateId);
      ps.setString(3, tokenHash);
      ps.setString(4, scannedAt.toString());
      ps.executeUpdate();
      try (var keys = ps.getGeneratedKeys()) {
        keys.next();
        return keys.getLong(1);
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public synchronized void update(
      UUID attemptId,
      String status,
      String employeeUuid,
      String jti,
      String direction,
      boolean handedOver,
      String result) {
    try (var ps =
        db.prepareStatement(
            "UPDATE journal SET status=?, employee_uuid=COALESCE(?, employee_uuid), jti=COALESCE(?, jti),"
                + " direction=COALESCE(?, direction), handed_over=?, result=? WHERE attempt_id=?")) {
      ps.setString(1, status);
      ps.setString(2, employeeUuid);
      ps.setString(3, jti);
      ps.setString(4, direction);
      ps.setInt(5, handedOver ? 1 : 0);
      ps.setString(6, result);
      ps.setString(7, attemptId.toString());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public synchronized Optional<Row> find(UUID attemptId) {
    return query("SELECT * FROM journal WHERE attempt_id = ?", attemptId.toString()).stream()
        .findFirst();
  }

  public synchronized List<Row> all() {
    return query("SELECT * FROM journal ORDER BY journal_seq");
  }

  /** Offline decisions the edge has not seen yet, oldest first. */
  public synchronized List<Row> pendingOfflineHandover() {
    return query(
        "SELECT * FROM journal WHERE handed_over = 0 AND status IN ('OFFLINE_OPENED','OFFLINE_DENIED')"
            + " ORDER BY journal_seq");
  }

  /**
   * Scans that never got any decision (crash or no answer): safe to ask the edge again with the
   * same id.
   */
  public synchronized List<Row> unfinished() {
    return query(
        "SELECT * FROM journal WHERE handed_over = 0 AND status = 'RECEIVED' ORDER BY journal_seq");
  }

  /**
   * ADR-12 R2: the local replay cache is simply "token ids seen recently" read from the journal.
   */
  public synchronized boolean jtiSeenSince(String jti, Instant since) {
    try (var ps =
        db.prepareStatement(
            "SELECT 1 FROM journal WHERE jti = ? AND scanned_at >= ? AND status IN ('OFFLINE_OPENED','OPENED') LIMIT 1")) {
      ps.setString(1, jti);
      ps.setString(2, since.toString());
      try (var rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public synchronized Optional<String> lastDirection(String employeeUuid) {
    try (var ps =
        db.prepareStatement(
            "SELECT direction FROM journal WHERE employee_uuid = ? AND status IN ('OFFLINE_OPENED','OPENED')"
                + " ORDER BY journal_seq DESC LIMIT 1")) {
      ps.setString(1, employeeUuid);
      try (var rs = ps.executeQuery()) {
        return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  private List<Row> query(String sql, String... args) {
    try (PreparedStatement ps = db.prepareStatement(sql)) {
      for (int i = 0; i < args.length; i++) {
        ps.setString(i + 1, args[i]);
      }
      var rows = new ArrayList<Row>();
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          rows.add(
              new Row(
                  rs.getLong("journal_seq"),
                  UUID.fromString(rs.getString("attempt_id")),
                  rs.getString("gate_id"),
                  rs.getString("token_hash"),
                  Instant.parse(rs.getString("scanned_at")),
                  rs.getString("status"),
                  rs.getString("employee_uuid"),
                  rs.getString("jti"),
                  rs.getString("direction"),
                  rs.getInt("handed_over") == 1,
                  rs.getString("result")));
        }
      }
      return rows;
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  // ---------- caches ----------

  public synchronized void saveDeviceKey(DeviceKey key) {
    try (var ps =
        db.prepareStatement(
            "INSERT INTO device_keys (device_id, employee_uuid, public_key, revoked) VALUES (?,?,?,?)"
                + " ON CONFLICT(device_id) DO UPDATE SET revoked = excluded.revoked, public_key = excluded.public_key")) {
      ps.setString(1, key.deviceId());
      ps.setString(2, key.employeeUuid());
      ps.setString(3, key.publicKeyBase64());
      ps.setInt(4, key.revoked() ? 1 : 0);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public synchronized Optional<DeviceKey> deviceKey(String deviceId) {
    try (var ps = db.prepareStatement("SELECT * FROM device_keys WHERE device_id = ?")) {
      ps.setString(1, deviceId);
      try (var rs = ps.executeQuery()) {
        return rs.next()
            ? Optional.of(
                new DeviceKey(
                    rs.getString("device_id"),
                    rs.getString("employee_uuid"),
                    rs.getString("public_key"),
                    rs.getInt("revoked") == 1))
            : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public synchronized void saveGate(Gate gate) {
    try (var ps =
        db.prepareStatement(
            "INSERT INTO gates (gate_id, mode) VALUES (?,?) ON CONFLICT(gate_id) DO UPDATE SET mode = excluded.mode")) {
      ps.setString(1, gate.gateId());
      ps.setString(2, gate.mode());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public synchronized Optional<Gate> gate(String gateId) {
    try (var ps = db.prepareStatement("SELECT * FROM gates WHERE gate_id = ?")) {
      ps.setString(1, gateId);
      try (var rs = ps.executeQuery()) {
        return rs.next()
            ? Optional.of(new Gate(rs.getString("gate_id"), rs.getString("mode")))
            : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public synchronized void setMeta(String key, String value) {
    try (var ps =
        db.prepareStatement(
            "INSERT INTO meta (k, v) VALUES (?,?) ON CONFLICT(k) DO UPDATE SET v = excluded.v")) {
      ps.setString(1, key);
      ps.setString(2, value);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public synchronized Optional<String> meta(String key) {
    try (var ps = db.prepareStatement("SELECT v FROM meta WHERE k = ?")) {
      ps.setString(1, key);
      try (var rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void close() {
    try {
      db.close();
    } catch (SQLException ignored) {
      // nothing useful to do
    }
  }
}
