package vn.tmc.hrintegration.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Sync state of one attendance attempt towards HR/Payroll (document.md 5.9), keyed by the attemptId
 * (eventId). batchId is both the Idempotency-Key and the event_id sent to HR: the attemptId for the
 * original line, the correctionId for a correction. Settled states are SYNCED and FAILED; UNKNOWN
 * means HR may or may not have the batch and must be looked up before any resend.
 *
 * @param line what is sent to HR; null only in records built by tests that do not need resending
 * @param nextAttemptAt earliest time the reconcile job may retry, null for "now"
 */
public record SyncRecord(
    UUID eventId,
    SyncStatus status,
    int attempts,
    String lastError,
    Instant updatedAt,
    long aggregateVersion,
    UUID batchId,
    YearMonth attendancePeriod,
    PayrollLine line,
    Instant nextAttemptAt) {

  private static final Duration FIRST_RETRY = Duration.ofMinutes(1);
  private static final Duration MAX_RETRY = Duration.ofHours(1);

  public SyncRecord(
      UUID eventId, SyncStatus status, int attempts, String lastError, Instant updatedAt) {
    this(eventId, status, attempts, lastError, updatedAt, 1L, eventId, null, null, null);
  }

  public SyncRecord(
      UUID eventId,
      SyncStatus status,
      int attempts,
      String lastError,
      Instant updatedAt,
      long aggregateVersion,
      UUID batchId,
      YearMonth attendancePeriod) {
    this(
        eventId,
        status,
        attempts,
        lastError,
        updatedAt,
        aggregateVersion,
        batchId,
        attendancePeriod,
        null,
        null);
  }

  /** Moves to {@code next}; IN_PROGRESS counts one more attempt. Clears any scheduled retry. */
  public SyncRecord with(SyncStatus next, String error, Instant now) {
    int nextAttempts = next == SyncStatus.IN_PROGRESS ? attempts + 1 : attempts;
    return new SyncRecord(
        eventId,
        next,
        nextAttempts,
        error,
        now,
        aggregateVersion,
        batchId,
        attendancePeriod,
        line,
        null);
  }

  /**
   * Moves to {@code next} and schedules the next retry with exponential backoff on the attempts
   * made so far: 1, 2, 4 ... minutes, at most one hour.
   */
  public SyncRecord retryLater(SyncStatus next, String error, Instant now) {
    long factor = 1L << Math.min(Math.max(attempts - 1, 0), 6);
    var delay = FIRST_RETRY.multipliedBy(factor);
    if (delay.compareTo(MAX_RETRY) > 0) {
      delay = MAX_RETRY;
    }
    return new SyncRecord(
        eventId,
        next,
        attempts,
        error,
        now,
        aggregateVersion,
        batchId,
        attendancePeriod,
        line,
        now.plus(delay));
  }

  public boolean isSettled() {
    return status == SyncStatus.SYNCED || status == SyncStatus.FAILED;
  }

  /** HR may already have this batch: look it up before sending it again. */
  public boolean isUncertain() {
    return status == SyncStatus.UNKNOWN || status == SyncStatus.IN_PROGRESS;
  }

  public boolean isDue(Instant now) {
    return nextAttemptAt == null || !nextAttemptAt.isAfter(now);
  }
}
