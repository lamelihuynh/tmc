package vn.tmc.userqr.application.port.out;

import java.util.UUID;

/**
 * Driven port: security-relevant facts (NFR-02). Identifiers only, never keys or personal data.
 * Default profile: {@code OutboxAuditLog}, an AuditRecorded event to audit-admin through the outbox
 * (C-08); inmem and local: {@code LoggingAuditLog}.
 */
public interface IAuditLog {
  void record(String action, UUID actor, String target, String result);
}
