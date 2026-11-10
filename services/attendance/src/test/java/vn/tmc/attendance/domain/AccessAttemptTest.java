package vn.tmc.attendance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.platform.kernel.ReasonCode;

class AccessAttemptTest {
  private final Instant now = Instant.parse("2026-11-09T06:45:00Z");

  private AccessAttempt newAttempt() {
    return AccessAttempt.validated(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "BD-GATE-01",
        "BD",
        Direction.ENTRY,
        "jti-1",
        AttemptSource.ONLINE,
        now,
        now);
  }

  @Test
  void should_walk_the_happy_path_to_committed() {
    var a = newAttempt();
    a.reserve(now);
    a.commandGate(now);
    a.gateOpened(now);
    a.commit(now);

    assertThat(a.status()).isEqualTo(AttemptStatus.COMMITTED);
    assertThat(a.countsAsAttendance()).isTrue();
  }

  @Test
  void should_not_allow_opening_before_the_gate_was_commanded() {
    var a = newAttempt();
    a.reserve(now);

    assertThatThrownBy(() -> a.gateOpened(now))
        .isInstanceOf(IllegalAttemptTransitionException.class);
  }

  @Test
  void should_resolve_unknown_to_opened_or_failed_but_not_back_to_commanded() {
    var a = newAttempt();
    a.reserve(now);
    a.commandGate(now);
    a.gateUnknown(now);

    assertThatThrownBy(() -> a.commandGate(now))
        .isInstanceOf(IllegalAttemptTransitionException.class);
    a.gateOpened(now);
    assertThat(a.status()).isEqualTo(AttemptStatus.GATE_OPENED);
  }

  @Test
  void should_treat_denied_and_failed_as_terminal() {
    var denied = newAttempt();
    denied.deny(ReasonCode.QR_EXPIRED, now);
    assertThat(denied.status().isTerminal()).isTrue();
    assertThatThrownBy(() -> denied.reserve(now))
        .isInstanceOf(IllegalAttemptTransitionException.class);

    var failed = newAttempt();
    failed.reserve(now);
    failed.commandGate(now);
    failed.gateFailed(ReasonCode.GATE_HARDWARE_ERROR, now);
    assertThat(failed.status().isTerminal()).isTrue();
  }

  @Test
  void should_not_count_a_committed_attempt_that_carries_an_anomaly() {
    var a = newAttempt();
    a.reserve(now);
    a.commandGate(now);
    a.gateOpened(now);
    a.commit(now);
    a.flagAnomaly(Anomaly.TOKEN_REUSED_OFFLINE);

    assertThat(a.countsAsAttendance()).isFalse();
  }
}
