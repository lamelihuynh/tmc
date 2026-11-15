package vn.tmc.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.platform.messaging.outbox.InMemoryOutboxStore;
import vn.tmc.platform.messaging.outbox.OutboxRelay;

class OutboxRelayTest {
  private final InMemoryOutboxStore outbox = new InMemoryOutboxStore();
  private final InMemoryEventBroker broker = new InMemoryEventBroker();
  private final OutboxRelay relay = new OutboxRelay(outbox, broker, 10);

  private vn.tmc.platform.kernel.EventEnvelope audit(String action) {
    return AuditEvents.recorded(
        TmcExchange.USER_QR, UUID.randomUUID(), action, "device-1", "OK", null, "c", Instant.now());
  }

  @Test
  void should_publish_in_order_and_keep_everything_pending_while_the_broker_is_down() {
    var a = audit("DEVICE_REGISTERED");
    var b = audit("DEVICE_REVOKED");
    outbox.add(a);
    outbox.add(b);
    broker.setAvailable(false);

    assertThat(relay.runOnce()).isEqualTo(new OutboxRelay.Report(0, 2));
    assertThat(outbox.failures(a.messageId())).isEqualTo(1);
    assertThat(outbox.failures(b.messageId())).isZero(); // stopped at the first failure

    broker.setAvailable(true);
    assertThat(relay.runOnce()).isEqualTo(new OutboxRelay.Report(2, 0));
    assertThat(broker.published())
        .extracting(e -> e.messageId())
        .containsExactly(a.messageId(), b.messageId());
    assertThat(relay.runOnce()).isEqualTo(new OutboxRelay.Report(0, 0));
  }

  @Test
  void should_build_audit_events_with_the_routing_source_and_no_empty_action() {
    var e = audit("LOGIN_LOCKED");

    assertThat(e.eventType()).isEqualTo("AuditRecorded");
    assertThat(e.source()).isEqualTo("user-qr");
    assertThat(e.payload()).containsEntry("action", "LOGIN_LOCKED").containsEntry("result", "OK");
    assertThat(TmcExchange.routingKey(e.source(), e.eventType()))
        .isEqualTo("user-qr.AuditRecorded");
    assertThatThrownBy(
            () ->
                AuditEvents.recorded("identity", null, " ", null, null, null, null, Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
