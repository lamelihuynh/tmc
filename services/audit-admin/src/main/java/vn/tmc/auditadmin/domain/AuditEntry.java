package vn.tmc.auditadmin.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * One audit fact: who did what, when, where from, with what result (NFR-02). Immutable. Details
 * carry identifiers only, never readable personal data (ADR-09).
 */
public record AuditEntry(
    UUID entryId,
    Instant occurredAt,
    String source,
    String action,
    String actor,
    String target,
    Map<String, String> details) {
  public AuditEntry {
    details = details == null ? Map.of() : Map.copyOf(details);
  }
}
