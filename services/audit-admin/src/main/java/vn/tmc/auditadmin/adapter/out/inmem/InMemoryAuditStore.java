package vn.tmc.auditadmin.adapter.out.inmem;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.auditadmin.application.port.out.IAuditStore;
import vn.tmc.auditadmin.domain.AuditEntry;

/** Process-memory audit log. TODO Team D: PostgreSQL with an INSERT-only role and a hash chain. */
public class InMemoryAuditStore implements IAuditStore {
  private final Map<UUID, AuditEntry> entries = new ConcurrentHashMap<>();

  @Override
  public void append(AuditEntry entry) {
    entries.putIfAbsent(entry.entryId(), entry);
  }

  @Override
  public List<AuditEntry> search(Instant from, Instant to, String action, String actor, int limit) {
    return entries.values().stream()
        .filter(e -> from == null || !e.occurredAt().isBefore(from))
        .filter(e -> to == null || e.occurredAt().isBefore(to))
        .filter(e -> action == null || action.equals(e.action()))
        .filter(e -> actor == null || actor.equals(e.actor()))
        .sorted(Comparator.comparing(AuditEntry::occurredAt).reversed())
        .limit(limit)
        .toList();
  }

  @Override
  public long count() {
    return entries.size();
  }
}
