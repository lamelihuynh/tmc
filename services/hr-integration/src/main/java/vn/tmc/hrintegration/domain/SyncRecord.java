package vn.tmc.hrintegration.domain;

import java.time.Instant;
import java.util.UUID;

public record SyncRecord(
    UUID eventId,
    SyncStatus status,
    int attempts,
    String lastError,
    Instant updatedAt,
    long aggregateVersion,
    UUID batchId) {

  public SyncRecord(
      UUID eventId, SyncStatus status, int attempts, String lastError, Instant updatedAt) {
    this(eventId, status, attempts, lastError, updatedAt, 1L, eventId);
  }

  public SyncRecord with(SyncStatus next, String error, Instant now) {
    int nextAttempts = attempts;
    if (next == SyncStatus.IN_PROGRESS) {
      nextAttempts++;
    }
    return new SyncRecord(eventId, next, nextAttempts, error, now, aggregateVersion, batchId);
  }

  public boolean isSettled() {
    return status == SyncStatus.SYNCED || status == SyncStatus.FAILED;
  }
}