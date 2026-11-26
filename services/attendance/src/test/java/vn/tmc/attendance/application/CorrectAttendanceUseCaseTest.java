package vn.tmc.attendance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import vn.tmc.attendance.adapter.out.inmem.InMemoryAttendanceCorrectionRepository;
import vn.tmc.attendance.adapter.out.inmem.InMemoryAttendanceTransactionRepository;
import vn.tmc.attendance.application.port.in.AttemptNotFoundException;
import vn.tmc.attendance.application.port.in.AttendanceNotCountedException;
import vn.tmc.attendance.application.port.in.CorrectionConflictException;
import vn.tmc.attendance.application.port.in.GateResult;
import vn.tmc.attendance.application.port.in.IAttendanceCommand.RecordAttemptCommand;
import vn.tmc.attendance.application.port.in.IAttendanceCorrection.CorrectAttendanceCommand;
import vn.tmc.attendance.application.usecase.AttendanceCommandService;
import vn.tmc.attendance.application.usecase.CorrectAttendanceUseCase;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.AttendanceCorrection;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.attendance.support.AttendanceFixture;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;

/** B-06 acceptance and idempotency regressions, on real in-memory adapters without Spring. */
class CorrectAttendanceUseCaseTest {
  private static final UUID HR_ID = UUID.fromString("0192a1c0-0000-7000-8000-00000000a003");
  private static final Principal HR = new Principal(HR_ID, Set.of(Role.HR_STAFF), DataScope.all());
  private final AttendanceFixture f = new AttendanceFixture();
  private final InMemoryAttendanceCorrectionRepository corrections =
      new InMemoryAttendanceCorrectionRepository();
  private final InMemoryAttendanceTransactionRepository transactions =
      new InMemoryAttendanceTransactionRepository(corrections);
  private final AttendanceCommandService command =
      new AttendanceCommandService(f.attempts, transactions, f.outbox, f.store, f.events, f.clock);
  private final CorrectAttendanceUseCase useCase =
      new CorrectAttendanceUseCase(
          f.attempts, transactions, corrections, f.outbox, f.store, f.events, f.clock);

  @Test
  void should_append_a_correction_and_both_events_without_overwriting_the_original() {
    var id = confirmedPass(AttemptSource.ONLINE, "BD", Direction.ENTRY, f.clock.now());
    var original = transactions.findByAttemptId(id).orElseThrow();
    var originalVersion = f.attempts.findById(id).orElseThrow().version();
    int before = f.outbox.all().size();
    var request = correction(id, Direction.EXIT, f.clock.now().minusSeconds(60));

    var result = useCase.correct(HR, request);

    assertThat(result.idempotentReplay()).isFalse();
    assertThat(result.aggregateVersion()).isEqualTo(originalVersion + 1);
    assertThat(transactions.findByAttemptId(id)).contains(original);
    assertThat(f.attempts.findById(id).orElseThrow().version()).isEqualTo(originalVersion);
    assertThat(corrections.forAttempt(id)).containsExactly(result.correction());
    assertThat(result.correction().originalAttemptId()).isEqualTo(id);
    assertThat(result.correction().previousDirection()).isEqualTo(Direction.ENTRY);
    assertThat(result.correction().previousScannedAt()).isEqualTo(original.occurredAt());
    assertThat(result.correction().correctedBy()).isEqualTo(HR_ID);
    var newEvents = f.outbox.all().subList(before, f.outbox.all().size());
    assertThat(newEvents)
        .extracting(e -> e.eventType())
        .containsExactly("AttendanceCorrected", "AuditRecorded");
    assertThat(newEvents)
        .allSatisfy(
            event -> {
              assertThat(event.eventId()).isEqualTo(id);
              assertThat(event.aggregateVersion()).isEqualTo(originalVersion + 1);
              assertThat(event.payload()).doesNotContainKeys("jti", "token", "qrToken", "name");
            });
    assertThat(newEvents.get(0).payload())
        .containsEntry("direction", "EXIT")
        .containsEntry("correctedBy", HR_ID.toString())
        .containsEntry("reason", "WRONG_DIRECTION");
    assertThat(newEvents.get(1).payload())
        .containsEntry("action", "ATTENDANCE_CORRECTED")
        .containsEntry("actor", HR_ID.toString())
        .containsEntry("reason", "WRONG_DIRECTION")
        .containsEntry("operationId", request.correctionId().toString());
  }

  @Test
  void should_return_the_saved_result_without_new_rows_or_events_on_replay() {
    var id = confirmedPass(AttemptSource.ONLINE, "BD", Direction.ENTRY, f.clock.now());
    var request = correction(id, Direction.EXIT, f.clock.now());
    var first = useCase.correct(HR, request);
    int eventCount = f.outbox.all().size();
    f.clock.advance(Duration.ofMinutes(1));

    var retry =
        useCase.correct(
            HR,
            new CorrectAttendanceCommand(
                request.correctionId(),
                id,
                request.direction(),
                request.scannedAt(),
                request.reason(),
                "different-retry-correlation"));

    assertThat(retry.idempotentReplay()).isTrue();
    assertThat(retry.correction()).isEqualTo(first.correction());
    assertThat(retry.aggregateVersion()).isEqualTo(first.aggregateVersion());
    assertThat(corrections.forAttempt(id)).hasSize(1);
    assertThat(f.outbox.all()).hasSize(eventCount);
  }

  @Test
  void should_chain_revisions_to_the_values_current_before_each_correction() {
    var id = confirmedPass(AttemptSource.ONLINE, "BD", Direction.ENTRY, f.clock.now());
    var firstRequest = correction(id, Direction.EXIT, f.clock.now().minusSeconds(60));
    var first = useCase.correct(HR, firstRequest);

    var second =
        useCase.correct(HR, correction(id, Direction.ENTRY, f.clock.now().plusSeconds(90)));
    int eventCount = f.outbox.all().size();
    var firstReplay = useCase.correct(HR, firstRequest);

    assertThat(second.correction().revision()).isEqualTo(2);
    assertThat(second.correction().previousDirection()).isEqualTo(first.correction().direction());
    assertThat(second.correction().previousScannedAt()).isEqualTo(first.correction().scannedAt());
    assertThat(second.aggregateVersion()).isEqualTo(first.aggregateVersion() + 1);
    assertThat(firstReplay.correction()).isEqualTo(first.correction());
    assertThat(firstReplay.aggregateVersion()).isEqualTo(first.aggregateVersion());
    assertThat(corrections.forAttempt(id)).hasSize(2);
    assertThat(f.outbox.all()).hasSize(eventCount);
  }

  @Test
  void should_reject_an_existing_key_with_changed_values_or_actor() {
    var id = confirmedPass(AttemptSource.ONLINE, "BD", Direction.ENTRY, f.clock.now());
    var request = correction(id, Direction.EXIT, f.clock.now());
    useCase.correct(HR, request);
    int eventCount = f.outbox.all().size();

    assertThatThrownBy(
            () ->
                useCase.correct(
                    HR,
                    new CorrectAttendanceCommand(
                        request.correctionId(),
                        id,
                        Direction.ENTRY,
                        request.scannedAt(),
                        request.reason(),
                        "c")))
        .isInstanceOf(CorrectionConflictException.class);
    assertThatThrownBy(
            () ->
                useCase.correct(
                    HR,
                    new CorrectAttendanceCommand(
                        request.correctionId(),
                        id,
                        request.direction(),
                        request.scannedAt().plusSeconds(1),
                        request.reason(),
                        "c")))
        .isInstanceOf(CorrectionConflictException.class);
    assertThatThrownBy(
            () ->
                useCase.correct(
                    HR,
                    new CorrectAttendanceCommand(
                        request.correctionId(),
                        id,
                        request.direction(),
                        request.scannedAt(),
                        "WRONG_TIME",
                        "c")))
        .isInstanceOf(CorrectionConflictException.class);
    var otherHr = new Principal(Ids.uuidV7(), Set.of(Role.HR_STAFF), DataScope.all());
    assertThatThrownBy(() -> useCase.correct(otherHr, request))
        .isInstanceOf(CorrectionConflictException.class);

    assertThat(corrections.forAttempt(id)).hasSize(1);
    assertThat(f.outbox.all()).hasSize(eventCount);
  }

  @Test
  void should_reject_a_key_already_used_for_another_attempt() {
    var first = confirmedPass(AttemptSource.ONLINE, "BD", Direction.ENTRY, f.clock.now());
    var second = confirmedPass(AttemptSource.ONLINE, "BD", Direction.EXIT, f.clock.now());
    var request = correction(first, Direction.EXIT, f.clock.now());
    useCase.correct(HR, request);
    int eventCount = f.outbox.all().size();

    assertThatThrownBy(
            () ->
                useCase.correct(
                    HR,
                    new CorrectAttendanceCommand(
                        request.correctionId(),
                        second,
                        request.direction(),
                        request.scannedAt(),
                        request.reason(),
                        "c")))
        .isInstanceOf(CorrectionConflictException.class);

    assertThat(corrections.forAttempt(second)).isEmpty();
    assertThat(f.outbox.all()).hasSize(eventCount);
  }

  @Test
  void should_refuse_an_unverified_manual_open_and_leave_it_uncounted() {
    var id = confirmedPass(AttemptSource.MANUAL, "BD", Direction.ENTRY, f.clock.now());
    int eventCount = f.outbox.all().size();

    assertThatThrownBy(() -> useCase.correct(HR, correction(id, Direction.EXIT, f.clock.now())))
        .isInstanceOf(AttendanceNotCountedException.class);

    assertThat(transactions.findByAttemptId(id)).isEmpty();
    assertThat(corrections.forAttempt(id)).isEmpty();
    assertThat(f.outbox.all()).hasSize(eventCount);
  }

  @Test
  void should_refuse_a_reserved_attempt_that_has_not_been_confirmed() {
    var id = Ids.uuidV7();
    command.recordAttempt(f.attemptCommand(id, "reserved-token", Direction.ENTRY));
    int eventCount = f.outbox.all().size();

    assertThatThrownBy(() -> useCase.correct(HR, correction(id, Direction.EXIT, f.clock.now())))
        .isInstanceOf(AttendanceNotCountedException.class);

    assertThat(corrections.forAttempt(id)).isEmpty();
    assertThat(f.outbox.all()).hasSize(eventCount);
  }

  @Test
  void should_require_hr_role_inside_the_use_case_before_any_repository_write() {
    var id = confirmedPass(AttemptSource.ONLINE, "BD", Direction.ENTRY, f.clock.now());
    var request = correction(id, Direction.EXIT, f.clock.now());
    int eventCount = f.outbox.all().size();

    for (Role role : Role.values()) {
      if (role != Role.HR_STAFF) {
        var caller = new Principal(HR_ID, Set.of(role), DataScope.all());
        assertThatThrownBy(() -> useCase.correct(caller, request))
            .isInstanceOf(AccessDeniedException.class);
      }
    }
    assertThatThrownBy(() -> useCase.correct(null, request))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () ->
                useCase.correct(
                    new Principal(null, Set.of(Role.HR_STAFF), DataScope.all()), request))
        .isInstanceOf(AccessDeniedException.class);

    assertThat(corrections.forAttempt(id)).isEmpty();
    assertThat(f.outbox.all()).hasSize(eventCount);
  }

  @Test
  void should_hide_out_of_scope_and_missing_attempts_from_scoped_hr() {
    var id = confirmedPass(AttemptSource.ONLINE, "LA", Direction.ENTRY, f.clock.now());
    var scopedHr = new Principal(HR_ID, Set.of(Role.HR_STAFF), DataScope.ofGates("BD"));
    int eventCount = f.outbox.all().size();

    assertThatThrownBy(
            () -> useCase.correct(scopedHr, correction(id, Direction.EXIT, f.clock.now())))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () ->
                useCase.correct(scopedHr, correction(Ids.uuidV7(), Direction.EXIT, f.clock.now())))
        .isInstanceOf(AccessDeniedException.class);

    assertThat(corrections.forAttempt(id)).isEmpty();
    assertThat(f.outbox.all()).hasSize(eventCount);
  }

  @Test
  void should_report_missing_attempt_to_hr_with_all_factory_access() {
    assertThatThrownBy(
            () -> useCase.correct(HR, correction(Ids.uuidV7(), Direction.EXIT, f.clock.now())))
        .isInstanceOf(AttemptNotFoundException.class);
    assertThat(f.outbox.all()).isEmpty();
  }

  @Test
  void should_reject_free_text_reasons_before_writing() {
    var id = confirmedPass(AttemptSource.ONLINE, "BD", Direction.ENTRY, f.clock.now());
    int eventCount = f.outbox.all().size();

    assertThatThrownBy(
            () ->
                new CorrectAttendanceCommand(
                    Ids.uuidV7(), id, Direction.ENTRY, f.clock.now(), "Employee name forgot", "c"))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(corrections.forAttempt(id)).isEmpty();
    assertThat(f.outbox.all()).hasSize(eventCount);
  }

  @Test
  void should_canonicalize_timestamp_precision_for_consistent_postgres_replay() {
    var id = confirmedPass(AttemptSource.ONLINE, "BD", Direction.ENTRY, f.clock.now());
    var request = correction(id, Direction.EXIT, Instant.parse("2026-11-09T06:44:00.123456789Z"));

    var first = useCase.correct(HR, request);
    var retry = useCase.correct(HR, request);

    assertThat(first.correction().scannedAt())
        .isEqualTo(Instant.parse("2026-11-09T06:44:00.123456Z"));
    assertThat(retry.idempotentReplay()).isTrue();
    assertThat(corrections.forAttempt(id)).hasSize(1);
  }

  @Test
  void should_order_and_filter_last_direction_using_latest_corrected_values() {
    var at = f.clock.now();
    var earlier = confirmedPass(AttemptSource.ONLINE, "BD", Direction.ENTRY, at.minusSeconds(600));
    var later = confirmedPass(AttemptSource.ONLINE, "BD", Direction.EXIT, at.minusSeconds(60));
    useCase.correct(HR, correction(earlier, Direction.EXIT, at.minusSeconds(30)));

    var last = transactions.latestBefore(AttendanceFixture.EMPLOYEE, at).orElseThrow();

    assertThat(last.attemptId()).isEqualTo(earlier);
    assertThat(last.direction()).isEqualTo(Direction.EXIT);
    assertThat(last.occurredAt()).isEqualTo(at.minusSeconds(30));
    // A later correction moves that pass beyond the cutoff. The next candidate must win.
    useCase.correct(HR, correction(earlier, Direction.ENTRY, at.plusSeconds(60)));
    assertThat(transactions.latestBefore(AttendanceFixture.EMPLOYEE, at).orElseThrow().attemptId())
        .isEqualTo(later);
    assertThat(transactions.findByAttemptId(earlier).orElseThrow().occurredAt())
        .isEqualTo(at.minusSeconds(600));
    assertThat(transactions.latestBefore(Ids.uuidV7(), at)).isEmpty();
  }

  @Test
  void should_use_a_strict_before_cutoff_after_a_correction() {
    var at = f.clock.now();
    var id = confirmedPass(AttemptSource.ONLINE, "BD", Direction.ENTRY, at.minusSeconds(60));
    useCase.correct(HR, correction(id, Direction.EXIT, at));

    assertThat(transactions.latestBefore(AttendanceFixture.EMPLOYEE, at)).isEmpty();
    assertThat(transactions.latestBefore(AttendanceFixture.EMPLOYEE, at.plusNanos(1)))
        .hasValueSatisfying(t -> assertThat(t.direction()).isEqualTo(Direction.EXIT));
  }

  @Test
  void should_serialize_two_concurrent_revisions_and_preserve_the_previous_values()
      throws Exception {
    var id = confirmedPass(AttemptSource.ONLINE, "BD", Direction.ENTRY, f.clock.now());
    var start = new CountDownLatch(1);
    int eventCount = f.outbox.all().size();
    var first = correction(id, Direction.EXIT, f.clock.now().minusSeconds(30));
    var second = correction(id, Direction.ENTRY, f.clock.now().minusSeconds(60));

    try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
      var a =
          workers.submit(
              () -> {
                start.await();
                return useCase.correct(HR, first);
              });
      var b =
          workers.submit(
              () -> {
                start.await();
                return useCase.correct(HR, second);
              });
      start.countDown();
      a.get(5, TimeUnit.SECONDS);
      b.get(5, TimeUnit.SECONDS);
    }

    var history = corrections.forAttempt(id);
    assertThat(history).extracting(AttendanceCorrection::revision).containsExactly(1, 2);
    assertThat(history.get(1).previousDirection()).isEqualTo(history.get(0).direction());
    assertThat(history.get(1).previousScannedAt()).isEqualTo(history.get(0).scannedAt());
    assertThat(f.outbox.all()).hasSize(eventCount + 4);
  }

  private CorrectAttendanceCommand correction(UUID attemptId, Direction direction, Instant at) {
    return new CorrectAttendanceCommand(
        Ids.uuidV7(), attemptId, direction, at, "WRONG_DIRECTION", "corr-hr-1");
  }

  private UUID confirmedPass(
      AttemptSource source, String factory, Direction direction, Instant at) {
    var id = Ids.uuidV7();
    command.recordAttempt(
        new RecordAttemptCommand(
            id,
            AttendanceFixture.EMPLOYEE,
            factory + "-GATE-01",
            factory,
            direction,
            "token-" + id,
            source,
            at,
            "setup"));
    command.markGateCommanded(id);
    command.recordGateResult(id, GateResult.OPENED, "setup");
    return id;
  }
}
