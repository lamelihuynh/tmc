package vn.tmc.reporting.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * One projected scan (one AccessAttempt). aggregateVersion counts state changes of that attempt
 * only, so it orders versions of the same scan (a correction has a higher one) and says nothing
 * about two different scans. Presence is derived from the latest scannedAt instead.
 */
public record ScanRecord(
    UUID attemptId,
    UUID employeeUuid,
    String factoryCode,
    String direction,
    Instant scannedAt,
    long aggregateVersion) {

  public ScanRecord {
    Objects.requireNonNull(attemptId, "attemptId");
    Objects.requireNonNull(employeeUuid, "employeeUuid");
    Objects.requireNonNull(factoryCode, "factoryCode");
    Objects.requireNonNull(direction, "direction");
    Objects.requireNonNull(scannedAt, "scannedAt");
    // PostgreSQL keeps microseconds: the same precision here keeps comparisons with stored rows
    // exact
    scannedAt = scannedAt.truncatedTo(ChronoUnit.MICROS);
  }

  /**
   * @return true if this is a newer version of the same scan as {@code stored}
   */
  public boolean replaces(ScanRecord stored) {
    return attemptId.equals(stored.attemptId) && aggregateVersion > stored.aggregateVersion;
  }

  public Presence toPresence() {
    return new Presence(employeeUuid, factoryCode, direction, scannedAt, aggregateVersion);
  }
}
