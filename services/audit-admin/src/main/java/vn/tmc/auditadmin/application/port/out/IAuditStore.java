package vn.tmc.auditadmin.application.port.out;

import java.time.Instant;
import java.util.List;
import vn.tmc.auditadmin.domain.AuditEntry;

/**
 * Driven port: append-only audit storage (NFR-02, AC-05). There is deliberately no method that
 * changes or removes history; AuditTest fails if one is added.
 */
public interface IAuditStore {
  /** Adds an entry. An entry whose id is already stored is ignored (redelivered message). */
  void append(AuditEntry entry);

  /**
   * @param from inclusive lower bound, null for no bound
   * @param to exclusive upper bound, null for no bound
   * @param action exact action name, null for any
   * @param actor exact actor, null for any
   * @return newest first, at most {@code limit} entries
   */
  List<AuditEntry> search(Instant from, Instant to, String action, String actor, int limit);

  long count();
}
