package vn.tmc.userqr.application.port.out;

import java.util.UUID;

/**
 * Driven port: security-relevant facts (NFR-02). Identifiers only, never keys or personal data.
 * TODO Team C (C-08): send through an outbox to audit-admin.
 */
public interface IAuditLog {
  void record(String action, UUID actor, String target, String result);
}
