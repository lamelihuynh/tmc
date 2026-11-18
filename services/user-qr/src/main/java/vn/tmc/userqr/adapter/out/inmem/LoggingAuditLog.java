package vn.tmc.userqr.adapter.out.inmem;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.tmc.userqr.application.port.out.IAuditLog;

/**
 * Writes audit facts to the log and keeps them in memory so tests can assert on them. TODO Team C
 * (C-08): replace with an outbox that delivers to audit-admin.
 */
public class LoggingAuditLog implements IAuditLog {
  private static final Logger LOG = LoggerFactory.getLogger("audit.user-qr");

  public record Entry(String action, UUID actor, String target, String result) {}

  private final List<Entry> entries = new CopyOnWriteArrayList<>();

  @Override
  public void record(String action, UUID actor, String target, String result) {
    entries.add(new Entry(action, actor, target, result));
    LOG.info("audit action={} actor={} target={} result={}", action, actor, target, result);
  }

  public List<Entry> entries() {
    return List.copyOf(entries);
  }
}
