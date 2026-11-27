package vn.tmc.auditadmin.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tmc.auditadmin.application.port.out.IAuditStore;
import vn.tmc.auditadmin.domain.AuditEntry;
import vn.tmc.auditadmin.domain.AuditHashChain;
import vn.tmc.auditadmin.domain.AuditHashChain.Link;

/**
 * Audit log in PostgreSQL (D-08, Flyway V2). The service logs in as a role that may only INSERT and
 * SELECT, and a trigger rejects UPDATE, DELETE and TRUNCATE even for the owner. Appends are
 * serialized by a transaction-scoped advisory lock (which needs no table privilege), so two writers
 * cannot both chain onto the same previous entry; UNIQUE(prev_hash) is the backstop. The lock joins
 * the consumer's transaction when there is one, so the entry commits with its dedup marker.
 */
public class JdbcAuditStore implements IAuditStore {
  /** Arbitrary constant shared by every writer of audit_entries. */
  static final long CHAIN_LOCK = 0x7A_4D_49_54L;

  private static final TypeReference<Map<String, String>> DETAILS = new TypeReference<>() {};
  private static final String COLUMNS =
      "seq, entry_id, occurred_at, source, action, actor, target, details::text AS details,"
          + " prev_hash, hash";

  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final ObjectMapper json;

  public JdbcAuditStore(JdbcTemplate jdbc, TransactionTemplate tx, ObjectMapper json) {
    this.jdbc = jdbc;
    this.tx = tx;
    this.json = json;
  }

  @Override
  public void append(AuditEntry entry) {
    tx.executeWithoutResult(
        status -> {
          jdbc.query("SELECT pg_advisory_xact_lock(?)", rs -> {}, CHAIN_LOCK);
          var exists =
              jdbc.queryForObject(
                  "SELECT count(*) FROM audit_entries WHERE entry_id = ?",
                  Integer.class,
                  entry.entryId());
          if (exists != null && exists > 0) {
            return; // redelivered: the first copy is already in the chain
          }
          var last =
              jdbc.query(
                  "SELECT hash FROM audit_entries ORDER BY seq DESC LIMIT 1",
                  (rs, row) -> rs.getString(1));
          var prevHash = last.isEmpty() ? AuditHashChain.GENESIS : last.get(0);
          jdbc.update(
              "INSERT INTO audit_entries"
                  + " (entry_id, occurred_at, source, action, actor, target, details, prev_hash,"
                  + " hash) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)",
              entry.entryId(),
              Timestamp.from(entry.occurredAt()),
              entry.source(),
              entry.action(),
              entry.actor(),
              entry.target(),
              toJson(entry.details()),
              prevHash,
              AuditHashChain.hash(prevHash, entry));
        });
  }

  @Override
  public List<AuditEntry> search(Instant from, Instant to, String action, String actor, int limit) {
    var sql = new StringBuilder("SELECT " + COLUMNS + " FROM audit_entries WHERE true");
    var args = new ArrayList<Object>();
    if (from != null) {
      sql.append(" AND occurred_at >= ?");
      args.add(Timestamp.from(from));
    }
    if (to != null) {
      sql.append(" AND occurred_at < ?");
      args.add(Timestamp.from(to));
    }
    if (action != null) {
      sql.append(" AND action = ?");
      args.add(action);
    }
    if (actor != null) {
      sql.append(" AND actor = ?");
      args.add(actor);
    }
    sql.append(" ORDER BY occurred_at DESC, seq DESC LIMIT ?");
    args.add(limit);
    return jdbc.query(sql.toString(), (rs, row) -> link(rs).entry(), args.toArray());
  }

  @Override
  public long count() {
    var n = jdbc.queryForObject("SELECT count(*) FROM audit_entries", Long.class);
    return n == null ? 0 : n;
  }

  @Override
  public AuditHashChain.Verification verifyChain() {
    var verifier = new AuditHashChain.Verifier();
    jdbc.query(
        "SELECT " + COLUMNS + " FROM audit_entries ORDER BY seq",
        rs -> {
          verifier.accept(link(rs));
        });
    return verifier.result();
  }

  private Link link(ResultSet rs) throws SQLException {
    var entry =
        new AuditEntry(
            rs.getObject("entry_id", UUID.class),
            rs.getTimestamp("occurred_at").toInstant(),
            rs.getString("source"),
            rs.getString("action"),
            rs.getString("actor"),
            rs.getString("target"),
            fromJson(rs.getString("details")));
    return new Link(rs.getLong("seq"), entry, rs.getString("prev_hash"), rs.getString("hash"));
  }

  private String toJson(Map<String, String> details) {
    try {
      return json.writeValueAsString(details);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("audit details are not serializable", e);
    }
  }

  private Map<String, String> fromJson(String text) {
    try {
      return text == null ? Map.of() : json.readValue(text, DETAILS);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("stored audit details are not valid JSON", e);
    }
  }
}
