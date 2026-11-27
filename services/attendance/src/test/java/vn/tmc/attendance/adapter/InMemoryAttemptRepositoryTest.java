package vn.tmc.attendance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.tmc.attendance.adapter.out.inmem.InMemoryAttemptRepository;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.kernel.Ids;

/** A late gate/recovery snapshot must not undo a newer HR verification. */
class InMemoryAttemptRepositoryTest {
  private final InMemoryAttemptRepository attempts = new InMemoryAttemptRepository();
  private final Instant now = Instant.parse("2026-11-20T01:00:00Z");
  private final UUID hr = UUID.fromString("0192a1c0-0000-7000-8000-00000000a003");

  @Test
  void should_reject_a_late_gate_result_without_erasing_hr_verification() {
    var a = committedManual();
    attempts.save(a);
    var stale = attempts.findById(a.attemptId()).orElseThrow();
    a.verifyManual(hr, "IDENTITY_CONFIRMED_BY_HR", now.plusSeconds(60));
    attempts.save(a);

    assertThatThrownBy(() -> attempts.save(stale)).isInstanceOf(IllegalStateException.class);

    var retained = attempts.findById(a.attemptId()).orElseThrow();
    assertThat(retained.version()).isEqualTo(a.version());
    assertThat(retained.countsAsAttendance()).isTrue();
    assertThat(retained.manualVerifiedBy()).isEqualTo(hr);
    assertThat(retained.manualVerifiedAt()).isEqualTo(now.plusSeconds(60));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void should_preserve_verification_even_if_a_stale_writer_reaches_the_same_or_higher_version(
      int extraVersion) {
    var verified = committedManual();
    verified.verifyManual(hr, "IDENTITY_CONFIRMED_BY_HR", now.plusSeconds(60));
    attempts.save(verified);
    var withoutEvidence =
        AccessAttempt.restore(
            verified.attemptId(),
            verified.employeeUuid(),
            verified.gateId(),
            verified.factoryCode(),
            verified.direction(),
            verified.jti(),
            verified.source(),
            verified.scannedAt(),
            verified.status(),
            verified.reasonCode(),
            verified.anomaly(),
            verified.updatedAt(),
            verified.version() + extraVersion);

    assertThatThrownBy(() -> attempts.save(withoutEvidence))
        .isInstanceOf(IllegalStateException.class);

    assertThat(attempts.findById(verified.attemptId()).orElseThrow().manualVerifiedBy())
        .isEqualTo(hr);
    assertThat(attempts.findById(verified.attemptId()).orElseThrow().version())
        .isEqualTo(verified.version());
  }

  @Test
  void should_allow_saving_an_unchanged_verified_snapshot() {
    var verified = committedManual();
    verified.verifyManual(hr, "IDENTITY_CONFIRMED_BY_HR", now.plusSeconds(60));
    attempts.save(verified);

    attempts.save(attempts.findById(verified.attemptId()).orElseThrow());

    assertThat(attempts.findById(verified.attemptId()).orElseThrow().manualVerifiedBy())
        .isEqualTo(hr);
  }

  private AccessAttempt committedManual() {
    return AccessAttempt.restore(
        Ids.uuidV7(),
        Ids.uuidV7(),
        "BD-GATE-01",
        "BD",
        Direction.ENTRY,
        "manual-test",
        AttemptSource.MANUAL,
        now,
        AttemptStatus.COMMITTED,
        null,
        null,
        now,
        5);
  }
}
