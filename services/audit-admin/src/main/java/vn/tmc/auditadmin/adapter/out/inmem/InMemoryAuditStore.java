package vn.tmc.auditadmin.adapter.out.inmem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import vn.tmc.auditadmin.application.port.out.IAuditStore;
import vn.tmc.auditadmin.domain.AuditEntry;
import vn.tmc.auditadmin.domain.AuditHashChain;
import vn.tmc.auditadmin.domain.AuditHashChain.Link;

/** Process-memory audit log for the inmem and local profiles; chained like the JDBC store. */
public class InMemoryAuditStore implements IAuditStore {
  private final List<Link> chain = new ArrayList<>();
  private final Set<UUID> ids = new HashSet<>();

  @Override
  public synchronized void append(AuditEntry entry) {
    if (!ids.add(entry.entryId())) {
      return;
    }
    var previous = chain.isEmpty() ? null : chain.get(chain.size() - 1);
    chain.add(AuditHashChain.next(previous, chain.size() + 1L, entry));
  }

  @Override
  public synchronized List<AuditEntry> search(
      Instant from, Instant to, String action, String actor, int limit) {
    return chain.stream()
        .map(Link::entry)
        .filter(e -> from == null || !e.occurredAt().isBefore(from))
        .filter(e -> to == null || e.occurredAt().isBefore(to))
        .filter(e -> action == null || action.equals(e.action()))
        .filter(e -> actor == null || actor.equals(e.actor()))
        .sorted(Comparator.comparing(AuditEntry::occurredAt).reversed())
        .limit(limit)
        .toList();
  }

  @Override
  public synchronized long count() {
    return chain.size();
  }

  @Override
  public synchronized AuditHashChain.Verification verifyChain() {
    return AuditHashChain.verify(List.copyOf(chain));
  }
}
