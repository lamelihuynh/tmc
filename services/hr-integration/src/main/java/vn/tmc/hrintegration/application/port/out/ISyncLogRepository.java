package vn.tmc.hrintegration.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.hrintegration.domain.SyncRecord;
import vn.tmc.hrintegration.domain.SyncStatus;

/** Driven port: the sync log, one row per attendance event (document.md 5.9). */
public interface ISyncLogRepository {
  Optional<SyncRecord> find(UUID eventId);

  void save(SyncRecord record);

  List<SyncRecord> byStatus(SyncStatus status);
}
