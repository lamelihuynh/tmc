package vn.tmc.gatestation.adapter.out.persistence;

import java.util.UUID;
import vn.tmc.gatestation.application.port.out.IAuditLog;
import vn.tmc.platform.messaging.outbox.AuditOutbox;

/** Writes gate audit facts as AuditRecorded messages in the Gate Store outbox. */
public class AuditOutboxAdapter implements IAuditLog {
  private final AuditOutbox audit;

  public AuditOutboxAdapter(AuditOutbox audit) {
    this.audit = audit;
  }

  @Override
  public void record(String action, UUID actor, String target, String detail) {
    audit.record(action, actor, target, detail);
  }
}
