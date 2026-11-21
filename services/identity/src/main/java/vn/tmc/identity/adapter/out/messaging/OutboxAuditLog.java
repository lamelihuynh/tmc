package vn.tmc.identity.adapter.out.messaging;

import java.util.UUID;
import vn.tmc.identity.application.port.out.IAuditLog;
import vn.tmc.platform.messaging.outbox.AuditOutbox;

/**
 * Login audit becomes {@code AuditRecorded} on {@code identity.AuditRecorded} through the outbox
 * (C-08, ADR-15); the relay publishes it to {@code tmc.events}.
 */
public class OutboxAuditLog implements IAuditLog {
  private final AuditOutbox outbox;

  public OutboxAuditLog(AuditOutbox outbox) {
    this.outbox = outbox;
  }

  @Override
  public void record(String action, UUID actor, String target, String result) {
    outbox.record(action, actor, target, result);
  }
}
