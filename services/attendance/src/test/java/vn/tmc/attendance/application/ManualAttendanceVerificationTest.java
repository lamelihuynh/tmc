package vn.tmc.attendance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.tmc.attendance.application.port.in.AttemptNotFoundException;
import vn.tmc.attendance.application.port.in.GateResult;
import vn.tmc.attendance.application.port.in.IAttendanceCommand.RecordAttemptCommand;
import vn.tmc.attendance.application.port.in.IManualAttendanceVerification.VerifyManualAttendanceCommand;
import vn.tmc.attendance.application.usecase.VerifyManualAttendanceUseCase;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.Anomaly;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.attendance.domain.ManualAttendanceNotVerifiableException;
import vn.tmc.attendance.support.AttendanceFixture;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;

/** B-07 keeps manual openings uncounted until HR verifies them, without reopening the gate. */
class ManualAttendanceVerificationTest {
  private static final UUID HR_ID = UUID.fromString("0192a1c0-0000-7000-8000-00000000a003");
  private static final String REASON = "IDENTITY_CONFIRMED_BY_HR";
  private final AttendanceFixture f = new AttendanceFixture();
  private final Principal hr = new Principal(HR_ID, Set.of(Role.HR_STAFF), DataScope.all());
  private final VerifyManualAttendanceUseCase verify =
      new VerifyManualAttendanceUseCase(
          f.attempts, f.transactions, f.outbox, f.store, f.events, f.clock);

  @Test
  void should_count_a_verified_manual_pass_and_publish_confirmation_with_audit() {
    var id = committedManual(AttendanceFixture.EMPLOYEE, Direction.ENTRY);
    var before = f.attempts.findById(id).orElseThrow();
    var eventCount = f.outbox.all().size();
    assertThat(before.countsAsAttendance()).isFalse();
    assertThat(f.transactions.all()).isEmpty();
    f.clock.advance(Duration.ofMinutes(5));

    var result = verify.verify(hr, command(id));

    var after = f.attempts.findById(id).orElseThrow();
    assertThat(result.idempotentReplay()).isFalse();
    assertThat(result.verifiedBy()).isEqualTo(HR_ID);
    assertThat(result.reason()).isEqualTo(REASON);
    assertThat(result.verifiedAt()).isEqualTo(f.clock.now());
    assertThat(result.aggregateVersion()).isEqualTo(before.version() + 1);
    assertThat(after.status()).isEqualTo(AttemptStatus.COMMITTED);
    assertThat(after.source()).isEqualTo(AttemptSource.MANUAL);
    assertThat(after.countsAsAttendance()).isTrue();
    assertThat(after.manualVerifiedBy()).isEqualTo(HR_ID);
    assertThat(f.transactions.all())
        .hasSize(1)
        .allSatisfy(
            t -> {
              assertThat(t.attemptId()).isEqualTo(id);
              assertThat(t.occurredAt()).isEqualTo(before.scannedAt());
              assertThat(t.source()).isEqualTo(AttemptSource.MANUAL);
            });
    var added = f.outbox.all().subList(eventCount, f.outbox.all().size());
    assertThat(added)
        .extracting(e -> e.eventType())
        .containsExactly("AttendanceConfirmed", "AuditRecorded");
    assertThat(added)
        .allSatisfy(
            e -> {
              assertThat(e.eventId()).isEqualTo(id);
              assertThat(e.aggregateVersion()).isEqualTo(result.aggregateVersion());
              assertThat(e.correlationId()).isEqualTo("corr-hr-verification");
              assertThat(e.payload()).doesNotContainKeys("jti", "token", "qrToken", "name");
            });
    assertThat(added.getFirst().payload())
        .containsEntry("status", "COMMITTED")
        .containsEntry("source", "MANUAL")
        .containsEntry("scannedAt", before.scannedAt().toString());
    assertThat(added.getLast().payload())
        .containsEntry("action", "MANUAL_ATTENDANCE_VERIFIED")
        .containsEntry("actor", HR_ID.toString())
        .containsEntry("reason", REASON);
    assertThat(f.transactions.latestBefore(AttendanceFixture.EMPLOYEE, f.clock.now()))
        .isPresent()
        .get()
        .extracting(t -> t.direction())
        .isEqualTo(Direction.ENTRY);
  }

  @Test
  void should_keep_original_evidence_and_create_nothing_when_verified_twice() {
    var id = committedManual(AttendanceFixture.EMPLOYEE, Direction.EXIT);
    var first = verify.verify(hr, command(id));
    var count = f.outbox.all().size();
    f.clock.advance(Duration.ofHours(1));
    var anotherHr = new Principal(Ids.uuidV7(), Set.of(Role.HR_STAFF), DataScope.all());

    var replay =
        verify.verify(
            anotherHr, new VerifyManualAttendanceCommand(id, "SECOND_REVIEW", "new-correlation"));

    assertThat(replay.idempotentReplay()).isTrue();
    assertThat(replay.verifiedAt()).isEqualTo(first.verifiedAt());
    assertThat(replay.verifiedBy()).isEqualTo(first.verifiedBy());
    assertThat(replay.reason()).isEqualTo(first.reason());
    assertThat(replay.aggregateVersion()).isEqualTo(first.aggregateVersion());
    assertThat(f.transactions.all()).hasSize(1);
    assertThat(f.outbox.all()).hasSize(count);
  }

  @Test
  void should_create_only_one_confirmation_when_verification_requests_overlap() throws Exception {
    var id = committedManual(AttendanceFixture.EMPLOYEE, Direction.ENTRY);
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () -> {
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                return verify.verify(hr, command(id));
              });
      var second =
          pool.submit(
              () -> {
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                return verify.verify(hr, command(id));
              });
      start.countDown();

      var results = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));

      assertThat(results)
          .extracting(r -> r.idempotentReplay())
          .containsExactlyInAnyOrder(false, true);
      assertThat(f.transactions.all()).hasSize(1);
      assertThat(f.outbox.all())
          .filteredOn(e -> e.eventType().equals("AttendanceConfirmed"))
          .hasSize(1);
      assertThat(f.outbox.all())
          .filteredOn(e -> "MANUAL_ATTENDANCE_VERIFIED".equals(e.payload().get("action")))
          .hasSize(1);
    }
  }

  @ParameterizedTest
  @EnumSource(value = Role.class, names = "HR_STAFF", mode = EnumSource.Mode.EXCLUDE)
  void should_deny_other_roles_even_when_they_have_global_scope(Role role) {
    var id = committedManual(AttendanceFixture.EMPLOYEE, Direction.ENTRY);
    var eventCount = f.outbox.all().size();
    var caller = new Principal(HR_ID, Set.of(role), DataScope.all());

    assertThatThrownBy(() -> verify.verify(caller, command(id)))
        .isInstanceOf(AccessDeniedException.class);

    assertUnchanged(id, eventCount);
  }

  @Test
  void should_deny_an_unauthenticated_caller() {
    assertThatThrownBy(() -> verify.verify(null, command(Ids.uuidV7())))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () ->
                verify.verify(
                    new Principal(null, Set.of(Role.HR_STAFF), DataScope.all()),
                    command(Ids.uuidV7())))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void should_enforce_factory_scope_before_verifying() {
    var id = committedManual(AttendanceFixture.EMPLOYEE, Direction.ENTRY);
    var eventCount = f.outbox.all().size();
    var laHr = new Principal(HR_ID, Set.of(Role.HR_STAFF), DataScope.ofGates("LA"));

    assertThatThrownBy(() -> verify.verify(laHr, command(id)))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> verify.verify(laHr, command(Ids.uuidV7())))
        .isInstanceOf(AccessDeniedException.class);

    assertUnchanged(id, eventCount);
  }

  @Test
  void should_report_a_missing_attempt_for_hr_with_global_scope() {
    assertThatThrownBy(() -> verify.verify(hr, command(Ids.uuidV7())))
        .isInstanceOf(AttemptNotFoundException.class);
    assertThat(f.outbox.all()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(value = AttemptStatus.class, names = "COMMITTED", mode = EnumSource.Mode.EXCLUDE)
  void should_not_verify_an_attempt_without_a_confirmed_opening(AttemptStatus status) {
    var id = Ids.uuidV7();
    f.attempts.save(
        AccessAttempt.restore(
            id,
            AttendanceFixture.EMPLOYEE,
            "BD-GATE-01",
            "BD",
            Direction.ENTRY,
            "manual-" + id,
            AttemptSource.MANUAL,
            f.clock.now(),
            status,
            null,
            null,
            f.clock.now(),
            4));

    assertThatThrownBy(() -> verify.verify(hr, command(id)))
        .isInstanceOf(ManualAttendanceNotVerifiableException.class);

    assertThat(f.attempts.findById(id).orElseThrow().status()).isEqualTo(status);
    assertUnchanged(id, 0);
  }

  @ParameterizedTest
  @EnumSource(value = AttemptSource.class, names = "MANUAL", mode = EnumSource.Mode.EXCLUDE)
  void should_not_verify_a_nonmanual_attempt(AttemptSource source) {
    var id = Ids.uuidV7();
    f.attempts.save(
        AccessAttempt.restore(
            id,
            AttendanceFixture.EMPLOYEE,
            "BD-GATE-01",
            "BD",
            Direction.ENTRY,
            "token-" + id,
            source,
            f.clock.now(),
            AttemptStatus.COMMITTED,
            null,
            null,
            f.clock.now(),
            5));

    assertThatThrownBy(() -> verify.verify(hr, command(id)))
        .isInstanceOf(ManualAttendanceNotVerifiableException.class);

    assertThat(f.attempts.findById(id).orElseThrow().manualVerifiedAt()).isNull();
    assertThat(f.transactions.all()).isEmpty();
    assertThat(f.outbox.all()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(Anomaly.class)
  void should_not_bypass_an_anomaly_when_verifying_manual_attendance(Anomaly anomaly) {
    var id = committedManual(AttendanceFixture.EMPLOYEE, Direction.ENTRY);
    var a = f.attempts.findById(id).orElseThrow();
    a.flagAnomaly(anomaly);
    f.attempts.save(a);
    var eventCount = f.outbox.all().size();

    assertThatThrownBy(() -> verify.verify(hr, command(id)))
        .isInstanceOf(ManualAttendanceNotVerifiableException.class);

    assertUnchanged(id, eventCount);
  }

  @Test
  void should_require_the_employee_and_direction_to_be_known() {
    var missingEmployee = committedManual(null, Direction.ENTRY);
    var missingDirection = committedManual(AttendanceFixture.EMPLOYEE, null);

    assertThatThrownBy(() -> verify.verify(hr, command(missingEmployee)))
        .isInstanceOf(ManualAttendanceNotVerifiableException.class);
    assertThatThrownBy(() -> verify.verify(hr, command(missingDirection)))
        .isInstanceOf(ManualAttendanceNotVerifiableException.class);

    assertThat(f.transactions.all()).isEmpty();
    assertThat(f.outbox.all()).extracting(e -> e.eventType()).doesNotContain("AttendanceConfirmed");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "confirmed by Alice", "lower_case", "A", "1INVALID"})
  void should_reject_free_text_or_invalid_reason_without_writing(String reason) {
    var id = committedManual(AttendanceFixture.EMPLOYEE, Direction.ENTRY);
    var count = f.outbox.all().size();

    assertThatThrownBy(
            () -> verify.verify(hr, new VerifyManualAttendanceCommand(id, reason, "corr")))
        .isInstanceOf(IllegalArgumentException.class);

    assertUnchanged(id, count);
  }

  @Test
  void should_reject_a_missing_command_or_attempt_id() {
    assertThatThrownBy(() -> verify.verify(hr, null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> verify.verify(hr, command(null)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_store_verification_time_at_postgresql_precision() {
    var id = committedManual(AttendanceFixture.EMPLOYEE, Direction.ENTRY);
    f.clock.advance(Duration.ofNanos(123456789));

    var result = verify.verify(hr, command(id));

    assertThat(result.verifiedAt().getNano()).isEqualTo(123456000);
    assertThat(f.attempts.findById(id).orElseThrow().manualVerifiedAt())
        .isEqualTo(result.verifiedAt());
  }

  private UUID committedManual(UUID employeeUuid, Direction direction) {
    var id = Ids.uuidV7();
    f.command.recordAttempt(
        new RecordAttemptCommand(
            id,
            employeeUuid,
            "BD-GATE-01",
            "BD",
            direction,
            "manual-" + id,
            AttemptSource.MANUAL,
            f.clock.now(),
            "corr-manual-open"));
    f.command.markGateCommanded(id);
    f.command.recordGateResult(id, GateResult.OPENED, "corr-manual-open");
    return id;
  }

  private VerifyManualAttendanceCommand command(UUID id) {
    return new VerifyManualAttendanceCommand(id, REASON, "corr-hr-verification");
  }

  private void assertUnchanged(UUID id, int eventCount) {
    assertThat(f.attempts.findById(id).orElseThrow().countsAsAttendance()).isFalse();
    assertThat(f.attempts.findById(id).orElseThrow().manualVerifiedAt()).isNull();
    assertThat(f.transactions.all()).isEmpty();
    assertThat(f.outbox.all()).hasSize(eventCount);
  }
}
