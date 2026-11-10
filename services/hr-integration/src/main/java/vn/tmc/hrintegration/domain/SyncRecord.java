package vn.tmc.hrintegration.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * One attendance event on its way to HR/Payroll. eventId is the idempotency key sent to HR
 * (ADR-05).
 */
public record SyncRecord(
    UUID eventId, SyncStatus status, int attempts, String lastError, Instant updatedAt) {

  public SyncRecord with(SyncStatus next, String error, Instant now) {
    return new SyncRecord(
        eventId, next, status == SyncStatus.IN_PROGRESS ? attempts : attempts + 1, error, now);
  }

  /**
   * SYNCED and FAILED are final for the automatic path: FAILED needs a human or a corrected resend.
   */
  public boolean isSettled() {
    return status == SyncStatus.SYNCED || status == SyncStatus.FAILED;
  }
}
