package vn.tmc.platform.messaging.outbox;

import java.util.UUID;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.messaging.AuditEvents;

/**
 * Writes AuditRecorded into a service's outbox (C-08). The services' {@code IAuditLog} ports have
 * the same shape, so an adapter is one line: {@code (action, actor, target, result) ->
 * auditOutbox.record(action, actor, target, result)}. Call it inside the transaction of the change
 * it describes when the outbox is a JdbcOutboxStore.
 */
public class AuditOutbox {
  private final OutboxStore outbox;
  private final String source;
  private final IClock clock;

  /**
   * @param source short service name, see {@link vn.tmc.platform.messaging.TmcExchange}
   */
  public AuditOutbox(OutboxStore outbox, String source, IClock clock) {
    this.outbox = outbox;
    this.source = source;
    this.clock = clock;
  }

  public void record(String action, UUID actor, String target, String result) {
    outbox.add(
        AuditEvents.recorded(source, null, action, target, result, actor, null, clock.now()));
  }
}
