package vn.tmc.hrintegration.adapter.out.inmem;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.hrintegration.application.port.out.ISyncLogRepository;
import vn.tmc.hrintegration.domain.SyncRecord;
import vn.tmc.hrintegration.domain.SyncStatus;

/** Process-memory sync log. TODO Team D: PostgreSQL table sync_log. */
public class InMemorySyncLogRepository implements ISyncLogRepository {
  private final Map<UUID, SyncRecord> rows = new ConcurrentHashMap<>();

  @Override
  public Optional<SyncRecord> find(UUID eventId) {
    return Optional.ofNullable(rows.get(eventId));
  }

  @Override
  public void save(SyncRecord record) {
    rows.put(record.eventId(), record);
  }

  @Override
  public List<SyncRecord> byStatus(SyncStatus status) {
    return rows.values().stream()
        .filter(r -> r.status() == status)
        .sorted(Comparator.comparing(SyncRecord::updatedAt))
        .toList();
  }
}
