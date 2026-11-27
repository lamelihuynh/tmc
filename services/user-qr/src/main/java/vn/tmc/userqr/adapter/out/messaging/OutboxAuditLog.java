package vn.tmc.userqr.adapter.out.messaging;

import java.util.UUID;
import vn.tmc.platform.messaging.outbox.AuditOutbox;
import vn.tmc.userqr.application.port.out.IAuditLog;

/**
 * Audit facts become {@code AuditRecorded} events on {@code user-qr.AuditRecorded} (C-08, ADR-15).
 * The row goes into the outbox table, so call it inside the transaction of the device change it
 * describes; the relay publishes it later. Only identifiers reach the event (ADR-09).
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
