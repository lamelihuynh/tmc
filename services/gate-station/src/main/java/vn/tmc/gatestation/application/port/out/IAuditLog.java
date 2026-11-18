package vn.tmc.gatestation.application.port.out;

import java.util.UUID;

/**
 * Driven port: security-relevant actions at the gate (NFR-02), for example a manual open and its
 * reason. TODO: deliver to audit-admin through an outbox instead of the log.
 */
public interface IAuditLog {
  void record(String action, UUID actor, String target, String detail);
}
