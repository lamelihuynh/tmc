package vn.tmc.attendance.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.attendance.application.port.in.GateResult;
import vn.tmc.attendance.application.port.in.IOfflineAttendanceIngest.LocalStatus;
import vn.tmc.attendance.application.port.in.IOfflineAttendanceIngest.OfflineRecord;
import vn.tmc.attendance.application.port.in.IOfflineAttendanceIngest.Outcome;
import vn.tmc.attendance.application.port.out.IGateStatusQuery.GateObservation;
import vn.tmc.attendance.domain.Anomaly;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.attendance.support.AttendanceFixture;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.kernel.ReasonCode;

/** The six mandatory tests of ADR-11 (compliance section), at the attendance-service level. */
class Adr11ResilienceTest {
  private final AttendanceFixture f = new AttendanceFixture();

  private long countType(String type) {
    return f.outbox.all().stream().filter(e -> e.eventType().equals(type)).count();
  }

  // ADR-11 test 1 (P1): same attempt_id twice gives exactly one record
  @Test
  void should_create_one_attempt_and_one_event_when_the_same_attempt_id_is_sent_twice() {
    var id = Ids.uuidV7();

    var first = f.command.recordAttempt(f.attemptCommand(id, "jti-1", Direction.ENTRY));
    var second = f.command.recordAttempt(f.attemptCommand(id, "jti-1", Direction.ENTRY));

    assertThat(first.duplicate()).isFalse();
    assertThat(second.duplicate()).isTrue();
    assertThat(countType("AccessAttemptRecorded")).isEqualTo(1);
  }

  @Test
  void should_not_rearm_the_gate_when_mark_commanded_is_replayed_after_a_result() {
    var id = Ids.uuidV7();
    f.command.recordAttempt(f.attemptCommand(id, "jti-1", Direction.ENTRY));
    f.command.markGateCommanded(id);
    f.command.recordGateResult(id, GateResult.OPENED, "c");

    var replay = f.command.markGateCommanded(id);

    assertThat(replay.duplicate()).isTrue();
    assertThat(replay.status()).isEqualTo(AttemptStatus.COMMITTED);
  }

  // ADR-11 test 2 (P3): timeout leaves UNKNOWN, reconcile resolves it without resending
  @Test
  void should_keep_unknown_then_commit_when_reconcile_confirms_the_gate_opened() {
    var id = Ids.uuidV7();
    f.command.recordAttempt(f.attemptCommand(id, "jti-1", Direction.ENTRY));
    f.command.markGateCommanded(id);

    var afterTimeout = f.command.recordGateResult(id, GateResult.UNKNOWN, "c");
    assertThat(afterTimeout.status()).isEqualTo(AttemptStatus.GATE_UNKNOWN);
    assertThat(f.transactions.all()).isEmpty();

    f.gateStatus.answer(id, GateObservation.OPENED);
    var report = f.reconcile.run();

    assertThat(report.committed()).isEqualTo(1);
    assertThat(f.attempts.findById(id).orElseThrow().status()).isEqualTo(AttemptStatus.COMMITTED);
    assertThat(f.transactions.all()).hasSize(1);
  }

  @Test
  void should_fail_the_attempt_when_reconcile_confirms_the_gate_never_opened() {
    var id = Ids.uuidV7();
    f.command.recordAttempt(f.attemptCommand(id, "jti-1", Direction.ENTRY));
    f.command.markGateCommanded(id);
    f.command.recordGateResult(id, GateResult.UNKNOWN, "c");
    f.gateStatus.answer(id, GateObservation.NOT_OPENED);

    var report = f.reconcile.run();

    assertThat(report.failed()).isEqualTo(1);
    assertThat(f.attempts.findById(id).orElseThrow().status()).isEqualTo(AttemptStatus.GATE_FAILED);
    assertThat(f.transactions.all()).isEmpty();
  }

  @Test
  void should_leave_attempt_unknown_for_the_guard_when_the_gate_state_is_undetermined() {
    var id = Ids.uuidV7();
    f.command.recordAttempt(f.attemptCommand(id, "jti-1", Direction.ENTRY));
    f.command.markGateCommanded(id);
    f.command.recordGateResult(id, GateResult.UNKNOWN, "c");

    var report = f.reconcile.run();

    assertThat(report.stillUnknown()).isEqualTo(1);
    assertThat(f.attempts.findById(id).orElseThrow().status())
        .isEqualTo(AttemptStatus.GATE_UNKNOWN);
  }

  // ADR-11 test 3 (P4): crash after GATE_COMMANDED, restart, nothing is lost
  @Test
  void should_recover_a_crashed_attempt_through_unknown_into_committed() {
    var id = Ids.uuidV7();
    f.command.recordAttempt(f.attemptCommand(id, "jti-1", Direction.ENTRY));
    f.command.markGateCommanded(id);
    // the process "crashes" here: no result was ever recorded
    f.clock.advance(Duration.ofSeconds(11));

    var recovered = f.recovery.run();
    assertThat(recovered.movedToUnknown()).isEqualTo(1);
    assertThat(f.attempts.findById(id).orElseThrow().status())
        .isEqualTo(AttemptStatus.GATE_UNKNOWN);

    f.gateStatus.answer(id, GateObservation.OPENED);
    f.reconcile.run();

    assertThat(f.attempts.findById(id).orElseThrow().status()).isEqualTo(AttemptStatus.COMMITTED);
    assertThat(f.transactions.all()).hasSize(1);
  }

  @Test
  void should_not_touch_a_commanded_attempt_inside_the_grace_period() {
    var id = Ids.uuidV7();
    f.command.recordAttempt(f.attemptCommand(id, "jti-1", Direction.ENTRY));
    f.command.markGateCommanded(id);
    f.clock.advance(Duration.ofSeconds(3));

    assertThat(f.recovery.run().movedToUnknown()).isZero();
  }

  // ADR-11 test 4 (P7): broker down after commit keeps the outbox, publish succeeds later
  @Test
  void should_keep_events_pending_while_the_broker_is_down_and_publish_them_once_it_returns() {
    var id = Ids.uuidV7();
    f.command.recordAttempt(f.attemptCommand(id, "jti-1", Direction.ENTRY));
    f.command.markGateCommanded(id);
    f.command.recordGateResult(id, GateResult.OPENED, "c");
    long total = f.outbox.pendingCount();

    f.broker.setAvailable(false);
    var down = f.publisher.run();
    assertThat(down.published()).isZero();
    assertThat(f.outbox.pendingCount()).isEqualTo(total);
    assertThat(f.attempts.findById(id).orElseThrow().status()).isEqualTo(AttemptStatus.COMMITTED);

    f.broker.setAvailable(true);
    var up = f.publisher.run();
    assertThat(up.published()).isEqualTo((int) total);
    assertThat(f.outbox.pendingCount()).isZero();
    assertThat(f.broker.published()).hasSize((int) total);
  }

  @Test
  void should_publish_events_in_the_order_they_were_written() {
    var id = Ids.uuidV7();
    f.command.recordAttempt(f.attemptCommand(id, "jti-1", Direction.ENTRY));
    f.command.markGateCommanded(id);
    f.command.recordGateResult(id, GateResult.OPENED, "c");

    f.publisher.run();

    assertThat(f.broker.published())
        .extracting(e -> e.eventType())
        .containsExactly("AccessAttemptRecorded", "GateResultRecorded", "AttendanceConfirmed");
  }

  // ADR-11 test 5 (P6): offline batch with duplicates and a token reused at two kiosks
  @Test
  void should_ingest_each_attempt_once_and_flag_a_token_reused_across_kiosks() {
    var a = Ids.uuidV7();
    var b = Ids.uuidV7();
    var c = Ids.uuidV7();
    var e2 = UUID.fromString("0192a1c0-0000-7000-8000-000000000002");
    var t0 = f.clock.now();

    var batch =
        List.of(
            offlineRecord(a, 1, AttendanceFixture.EMPLOYEE, "jti-A", Direction.ENTRY, t0),
            offlineRecord(
                a,
                2,
                AttendanceFixture.EMPLOYEE,
                "jti-A",
                Direction.ENTRY,
                t0), // duplicate attempt_id
            offlineRecord(
                b, 3, e2, "jti-A", Direction.ENTRY, t0.plusSeconds(5))); // same token, other person

    var results = f.offline.ingest("BD-KIOSK-01", batch);

    assertThat(results)
        .extracting(r -> r.outcome())
        .containsExactly(Outcome.ACCEPTED, Outcome.DUPLICATE, Outcome.ANOMALY);
    assertThat(f.transactions.all()).hasSize(1);
    assertThat(f.attempts.findById(b).orElseThrow().anomaly())
        .isEqualTo(Anomaly.TOKEN_REUSED_OFFLINE);
    assertThat(f.attempts.findById(b).orElseThrow().countsAsAttendance()).isFalse();
    assertThat(f.attempts.findById(c)).isEmpty();
  }

  @Test
  void should_flag_anti_passback_when_an_offline_record_repeats_the_last_direction() {
    var first = Ids.uuidV7();
    var second = Ids.uuidV7();
    var t0 = f.clock.now();

    var results =
        f.offline.ingest(
            "BD-KIOSK-01",
            List.of(
                offlineRecord(first, 1, AttendanceFixture.EMPLOYEE, "jti-1", Direction.ENTRY, t0),
                offlineRecord(
                    second,
                    2,
                    AttendanceFixture.EMPLOYEE,
                    "jti-2",
                    Direction.ENTRY,
                    t0.plusSeconds(60))));

    assertThat(results)
        .extracting(r -> r.outcome())
        .containsExactly(Outcome.ACCEPTED, Outcome.ANOMALY);
    assertThat(f.attempts.findById(second).orElseThrow().anomaly())
        .isEqualTo(Anomaly.ANTI_PASSBACK_OFFLINE);
  }

  @Test
  void should_process_offline_records_in_journal_order_not_arrival_order() {
    var entry = Ids.uuidV7();
    var exit = Ids.uuidV7();
    var t0 = f.clock.now();

    var results =
        f.offline.ingest(
            "BD-KIOSK-01",
            List.of(
                offlineRecord(
                    exit,
                    2,
                    AttendanceFixture.EMPLOYEE,
                    "jti-2",
                    Direction.EXIT,
                    t0.plusSeconds(60)),
                offlineRecord(entry, 1, AttendanceFixture.EMPLOYEE, "jti-1", Direction.ENTRY, t0)));

    assertThat(results).extracting(r -> r.attemptId()).containsExactly(entry, exit);
    assertThat(results).extracting(r -> r.outcome()).containsOnly(Outcome.ACCEPTED);
  }

  @Test
  void should_never_send_a_gate_command_when_ingesting_offline_records() {
    // the ingest use case has no dependency on any turnstile port; this asserts it also produces no
    // GATE_COMMANDED leftovers that the recovery job could pick up later.
    var id = Ids.uuidV7();
    f.offline.ingest(
        "BD-KIOSK-01",
        List.of(
            offlineRecord(
                id, 1, AttendanceFixture.EMPLOYEE, "jti-1", Direction.ENTRY, f.clock.now())));
    f.clock.advance(Duration.ofMinutes(5));

    var report = f.recovery.run();

    assertThat(report.movedToUnknown()).isZero();
    assertThat(f.attempts.findById(id).orElseThrow().status()).isEqualTo(AttemptStatus.COMMITTED);
  }

  // ADR-11 test 6 (P2): reservation older than the TTL is released
  @Test
  void should_release_a_stale_reservation_as_denied_system_retry() {
    var id = Ids.uuidV7();
    f.command.recordAttempt(f.attemptCommand(id, "jti-1", Direction.ENTRY));
    f.clock.advance(Duration.ofSeconds(31));

    var report = f.recovery.run();

    assertThat(report.releasedReservations()).isEqualTo(1);
    var attempt = f.attempts.findById(id).orElseThrow();
    assertThat(attempt.status()).isEqualTo(AttemptStatus.DENIED);
    assertThat(attempt.reasonCode()).isEqualTo(ReasonCode.SYSTEM_RETRY);
  }

  @Test
  void should_return_the_stored_denial_when_a_denied_request_is_retried() {
    var id = Ids.uuidV7();
    var cmd =
        new vn.tmc.attendance.application.port.in.IAttendanceCommand.RecordDeniedCommand(
            id,
            null,
            "BD-GATE-01",
            "BD",
            null,
            null,
            f.clock.now(),
            ReasonCode.INVALID_SIGNATURE,
            "c");

    var first = f.command.recordDenied(cmd);
    var second = f.command.recordDenied(cmd);

    assertThat(first.status()).isEqualTo(AttemptStatus.DENIED);
    assertThat(second.duplicate()).isTrue();
    assertThat(second.reasonCode()).isEqualTo(ReasonCode.INVALID_SIGNATURE);
  }

  @Test
  void should_not_leak_the_token_id_or_any_name_into_event_payloads() {
    var id = Ids.uuidV7();
    f.command.recordAttempt(f.attemptCommand(id, "secret-jti-123", Direction.ENTRY));
    f.command.markGateCommanded(id);
    f.command.recordGateResult(id, GateResult.OPENED, "c");

    for (var event : f.outbox.all()) {
      assertThat(event.payload().toString()).doesNotContain("secret-jti-123");
      assertThat(event.payload()).doesNotContainKeys("name", "fullName", "jti", "token");
    }
  }

  private static OfflineRecord offlineRecord(
      UUID attemptId, long seq, UUID employee, String jti, Direction dir, java.time.Instant at) {
    return new OfflineRecord(
        attemptId,
        seq,
        employee,
        "BD-GATE-01",
        "BD",
        dir,
        jti,
        at,
        LocalStatus.OFFLINE_OPENED,
        null);
  }
}
