package vn.tmc.hrintegration.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.hrintegration.domain.SyncRecord;
import vn.tmc.hrintegration.domain.SyncStatus;

/** Driven port: the sync log, one row per attendance attempt (document.md 5.9). */
public interface ISyncLogRepository {
  Optional<SyncRecord> find(UUID eventId);

  /** Inserts or replaces the row of {@code record.eventId()}. */
  void save(SyncRecord record);

  List<SyncRecord> byStatus(SyncStatus status);

  /**
   * Rows the reconcile job should retry now, oldest first: UNKNOWN and RETRY_PENDING whose
   * nextAttemptAt has passed, and IN_PROGRESS rows last touched before {@code stuckBefore} (the
   * process stopped during a send).
   */
  List<SyncRecord> dueForRetry(Instant now, Instant stuckBefore, int limit);
}
