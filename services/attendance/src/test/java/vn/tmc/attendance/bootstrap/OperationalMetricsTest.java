package vn.tmc.attendance.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.attendance.adapter.out.inmem.InMemoryAttemptRepository;
import vn.tmc.attendance.adapter.out.inmem.InMemoryOutboxRepository;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.attendance.support.MutableClock;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.Ids;

/** B-08: verifies both operational Micrometer gauges via MeterRegistry with controllable clock. */
class OperationalMetricsTest {

  @Test
  void should_count_only_pending_outbox_rows_older_than_five_minutes() {
    var clock = new MutableClock();
    var outbox = new InMemoryOutboxRepository();
    var attempts = new InMemoryAttemptRepository();
    var registry = new SimpleMeterRegistry();
    AttendanceMetricsConfig.bind(registry, outbox, attempts, clock);

    Instant now = clock.now();
    var freshPending = envelope(now.minus(Duration.ofMinutes(2)));
    var stalePending = envelope(now.minus(Duration.ofMinutes(6)));
    var stalePublished = envelope(now.minus(Duration.ofMinutes(10)));

    outbox.add(freshPending);
    assertThat(registry.get(AttendanceMetricsConfig.OUTBOX_STALE_METRIC).gauge().value()).isZero();

    outbox.add(stalePending);
    outbox.add(stalePublished);
    outbox.markPublished(stalePublished.messageId());

    assertThat(registry.get(AttendanceMetricsConfig.OUTBOX_STALE_METRIC).gauge().value())
        .isEqualTo(1.0);

    outbox.markPublished(stalePending.messageId());
    assertThat(registry.get(AttendanceMetricsConfig.OUTBOX_STALE_METRIC).gauge().value()).isZero();
  }

  @Test
  void should_count_only_attempts_currently_in_gate_unknown_status() {
    var clock = new MutableClock();
    var outbox = new InMemoryOutboxRepository();
    var attempts = new InMemoryAttemptRepository();
    var registry = new SimpleMeterRegistry();
    AttendanceMetricsConfig.bind(registry, outbox, attempts, clock);

    Instant now = clock.now();
    var reserved = newAttempt(now);
    reserved.reserve(now);
    attempts.save(reserved);

    var committed = newAttempt(now);
    committed.reserve(now);
    committed.commandGate(now);
    committed.gateOpened(now);
    committed.commit(now);
    attempts.save(committed);

    assertThat(registry.get(AttendanceMetricsConfig.GATE_UNKNOWN_METRIC).gauge().value()).isZero();

    var unknown1 = newAttempt(now);
    unknown1.reserve(now);
    unknown1.commandGate(now);
    unknown1.gateUnknown(now);
    attempts.save(unknown1);

    var unknown2 = newAttempt(now);
    unknown2.reserve(now);
    unknown2.commandGate(now);
    unknown2.gateUnknown(now);
    attempts.save(unknown2);

    assertThat(registry.get(AttendanceMetricsConfig.GATE_UNKNOWN_METRIC).gauge().value())
        .isEqualTo(2.0);

    unknown1.gateOpened(now);
    unknown1.commit(now);
    attempts.save(unknown1);

    assertThat(registry.get(AttendanceMetricsConfig.GATE_UNKNOWN_METRIC).gauge().value())
        .isEqualTo(1.0);
  }

  private static EventEnvelope envelope(Instant occurredAt) {
    return new EventEnvelope(
        Ids.uuidV7(),
        "AccessAttemptRecorded",
        1,
        Ids.uuidV7(),
        1,
        occurredAt,
        "corr-b08",
        "attendance",
        "BD",
        "BD-GATE-01",
        null);
  }

  private static AccessAttempt newAttempt(Instant now) {
    return AccessAttempt.validated(
        Ids.uuidV7(),
        UUID.randomUUID(),
        "BD-GATE-01",
        "BD",
        Direction.ENTRY,
        "jti-" + UUID.randomUUID(),
        AttemptSource.ONLINE,
        now,
        now);
  }
}
