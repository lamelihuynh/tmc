package vn.tmc.gatestation.application.port.out;

import java.util.UUID;

/**
 * Driven port: security-relevant actions at the gate (NFR-02), for example a manual open and its
 * reason. Production writes AuditRecorded events to Gate Store's outbox.
 */
public interface IAuditLog {
  void record(String action, UUID actor, String target, String detail);
}
