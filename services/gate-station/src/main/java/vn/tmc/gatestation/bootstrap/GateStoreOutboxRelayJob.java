package vn.tmc.gatestation.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import vn.tmc.platform.messaging.outbox.OutboxRelay;

/** Periodically sends confirmed Gate Store outbox events to the shared RabbitMQ exchange. */
class GateStoreOutboxRelayJob {
  private static final Logger LOG = LoggerFactory.getLogger(GateStoreOutboxRelayJob.class);

  private final OutboxRelay relay;

  GateStoreOutboxRelayJob(OutboxRelay relay) {
    this.relay = relay;
  }

  @Scheduled(fixedDelayString = "${tmc.outbox.relay-interval-ms:2000}")
  void relay() {
    var report = relay.runOnce();
    if (report.stillPending() > 0) {
      LOG.warn("gate outbox: {} events still pending (broker unavailable?)", report.stillPending());
    }
  }
}
