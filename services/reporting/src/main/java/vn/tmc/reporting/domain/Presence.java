package vn.tmc.reporting.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-model row: where an employee is right now, per factory (FR-22). Built only from
 * AttendanceConfirmed events, never by reading the Attendance database (ADR-10). aggregateVersion
 * guards against old messages overwriting newer state (document.md 12.7).
 */
public record Presence(
    UUID employeeUuid, String factoryCode, String direction, Instant at, long aggregateVersion) {

  public boolean isInside() {
    return "ENTRY".equals(direction);
  }

  /**
   * @return true if the candidate is newer than this row and should replace it
   */
  public boolean supersededBy(Presence candidate) {
    return candidate.aggregateVersion > aggregateVersion
        || (candidate.aggregateVersion == aggregateVersion && candidate.at.isAfter(at));
  }
}
