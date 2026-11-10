package vn.tmc.auditadmin.application.usecase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import vn.tmc.auditadmin.application.port.out.IAuditStore;
import vn.tmc.auditadmin.domain.AuditEntry;
import vn.tmc.platform.kernel.EventEnvelope;

/**
 * Turns AuditRecorded events into stored entries. The entry id is the event's messageId, so a
 * redelivered message cannot create a second entry even without the consumer dedupe table. TODO
 * Team D: forward audit from User/QR, Gate Station and Identity through their own outboxes; hash
 * chain for tamper evidence (AC-05).
 */
public class RecordAuditUseCase {
  private final IAuditStore store;

  public RecordAuditUseCase(IAuditStore store) {
    this.store = store;
  }

  public void handle(EventEnvelope event) {
    if (!"AuditRecorded".equals(event.eventType())) {
      return;
    }
    Map<String, String> details = new HashMap<>();
    event.payload().forEach((k, v) -> details.put(k, String.valueOf(v)));
    store.append(
        new AuditEntry(
            event.messageId(),
            event.occurredAt(),
            event.source(),
            details.getOrDefault("action", "UNKNOWN"),
            details.get("employeeUuid"),
            event.eventId().toString(),
            details));
  }

  /** Writes an entry about something that happened inside this service (for example a search). */
  public void recordLocal(String action, String actor, String target, Instant at) {
    store.append(
        new AuditEntry(
            vn.tmc.platform.kernel.Ids.uuidV7(),
            at,
            "audit-admin",
            action,
            actor,
            target,
            Map.of()));
  }
}
