package vn.tmc.gatestation.adapter.out.inmem;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.tmc.gatestation.application.port.out.IAuditLog;

/** Writes audit facts to the log and keeps them in memory so tests can assert on them. */
public class LoggingAuditLog implements IAuditLog {
  private static final Logger LOG = LoggerFactory.getLogger("audit.gate-station");

  public record Entry(String action, UUID actor, String target, String detail) {}

  private final List<Entry> entries = new CopyOnWriteArrayList<>();

  @Override
  public void record(String action, UUID actor, String target, String detail) {
    entries.add(new Entry(action, actor, target, detail));
    LOG.info("audit action={} actor={} target={} detail={}", action, actor, target, detail);
  }

  public List<Entry> entries() {
    return List.copyOf(entries);
  }
}
