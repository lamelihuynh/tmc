package vn.tmc.reporting.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Where an employee is right now in one factory (FR-22): the latest projected scan there, by
 * scannedAt. Built only from attendance events, never by reading the Attendance database (ADR-10).
 */
public record Presence(
    UUID employeeUuid, String factoryCode, String direction, Instant at, long aggregateVersion) {

  public boolean isInside() {
    return "ENTRY".equals(direction);
  }
}
