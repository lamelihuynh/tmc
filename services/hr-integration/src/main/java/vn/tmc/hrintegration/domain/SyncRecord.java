package vn.tmc.hrintegration.domain;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

public record SyncRecord(
    UUID eventId,
    SyncStatus status,
    int attempts,
    String lastError,
    Instant updatedAt,
    long aggregateVersion,
    UUID batchId,
    YearMonth attendancePeriod) {

  public SyncRecord(
      UUID eventId, SyncStatus status, int attempts, String lastError, Instant updatedAt) {
    this(eventId, status, attempts, lastError, updatedAt, 1L, eventId, null);
  }

  public SyncRecord(
      UUID eventId, SyncStatus status, int attempts, String lastError, Instant updatedAt,
      long aggregateVersion, UUID batchId) {
    this(eventId, status, attempts, lastError, updatedAt, aggregateVersion, batchId, null);
  }

  public SyncRecord with(SyncStatus next, String error, Instant now) {
    int nextAttempts = next == SyncStatus.IN_PROGRESS ? attempts + 1 : attempts;
    return new SyncRecord(
        eventId, next, nextAttempts, error, now, aggregateVersion, batchId, attendancePeriod);
  }

  public boolean isSettled() {
    return status == SyncStatus.SYNCED || status == SyncStatus.FAILED;
  }
}