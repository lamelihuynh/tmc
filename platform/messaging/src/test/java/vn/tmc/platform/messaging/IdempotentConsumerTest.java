package vn.tmc.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.Ids;

class IdempotentConsumerTest {

  private EventEnvelope envelope() {
    return new EventEnvelope(
        Ids.uuidV7(),
        "AttendanceConfirmed",
        1,
        Ids.uuidV7(),
        1,
        Instant.now(),
        "corr",
        "test",
        "BD",
        "G1",
        null);
  }

  @Test
  void should_run_handler_once_when_same_message_is_redelivered() {
    List<EventEnvelope> handled = new ArrayList<>();
    var consumer =
        new IdempotentConsumer("reporting", new InMemoryProcessedMessageStore(), handled::add);
    var msg = envelope();

    assertThat(consumer.accept(msg)).isTrue();
    assertThat(consumer.accept(msg)).isFalse();

    assertThat(handled).hasSize(1);
  }

  @Test
  void should_process_same_message_independently_per_consumer() {
    var store = new InMemoryProcessedMessageStore();
    List<EventEnvelope> a = new ArrayList<>();
    List<EventEnvelope> b = new ArrayList<>();
    var msg = envelope();

    new IdempotentConsumer("reporting", store, a::add).accept(msg);
    new IdempotentConsumer("hr", store, b::add).accept(msg);

    assertThat(a).hasSize(1);
    assertThat(b).hasSize(1);
  }
}
