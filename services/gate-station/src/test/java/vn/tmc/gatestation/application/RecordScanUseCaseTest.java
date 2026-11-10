package vn.tmc.gatestation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import vn.tmc.gatestation.adapter.out.simulated.SimulatedTurnstileAdapter.Fault;
import vn.tmc.gatestation.application.port.in.IGateAccess.Outcome;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.State;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.Trigger;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.support.GateFixture;
import vn.tmc.platform.kernel.ReasonCode;

class RecordScanUseCaseTest {
  private final GateFixture f = new GateFixture();

  @Test
  void should_open_the_gate_and_commit_when_everything_is_valid() {
    f.token("t1", GateFixture.E1, "jti-1");
    var req = f.request("BD-GATE-01", "t1");

    var result = f.scan.scan(req);

    assertThat(result.outcome()).isEqualTo(Outcome.ALLOWED);
    assertThat(result.direction()).isEqualTo(Direction.ENTRY);
    assertThat(f.turnstile.openCalls()).isEqualTo(1);
    assertThat(f.turnstile.lastRequest().commandKey()).isEqualTo(req.attemptId().toString());
    assertThat(f.turnstile.lastRequest().trigger()).isEqualTo(Trigger.QR_SCAN);
    assertThat(f.attendance.state(req.attemptId()).orElseThrow().state())
        .isEqualTo(State.COMMITTED);
  }

  // ---------- denials: the gate must stay closed ----------

  @Test
  void should_deny_and_not_touch_the_gate_when_the_qr_is_invalid() {
    f.qr.deny("bad", ReasonCode.INVALID_SIGNATURE);
    var req = f.request("BD-GATE-01", "bad");

    var result = f.scan.scan(req);

    assertThat(result.outcome()).isEqualTo(Outcome.DENIED);
    assertThat(result.reason()).isEqualTo(ReasonCode.INVALID_SIGNATURE);
    assertThat(f.turnstile.openCalls()).isZero();
    assertThat(f.attendance.state(req.attemptId()).orElseThrow().state()).isEqualTo(State.DENIED);
  }

  @Test
  void should_deny_an_unknown_token_as_malformed() {
    assertThat(f.scan.scan(f.request("BD-GATE-01", "never-seen")).reason())
        .isEqualTo(ReasonCode.MALFORMED_TOKEN);
  }

  @Test
  void should_deny_an_inactive_employee() {
    f.token("t", GateFixture.E3, "jti-x");

    assertThat(f.scan.scan(f.request("BD-GATE-01", "t")).reason())
        .isEqualTo(ReasonCode.EMPLOYEE_INACTIVE);
    assertThat(f.turnstile.openCalls()).isZero();
  }

  @Test
  void should_deny_an_employee_without_permission_for_this_factory() {
    f.token("t", GateFixture.E2, "jti-x"); // E2 may only enter LA

    assertThat(f.scan.scan(f.request("BD-GATE-01", "t")).reason())
        .isEqualTo(ReasonCode.FACTORY_NOT_PERMITTED);
  }

  @Test
  void should_deny_when_the_gate_is_in_maintenance_or_unknown() {
    f.token("t", GateFixture.E1, "jti-m");

    assertThat(f.scan.scan(f.request("BD-GATE-MT", "t")).reason())
        .isEqualTo(ReasonCode.GATE_IN_MAINTENANCE);
    assertThat(f.scan.scan(f.request("NOPE", "t")).reason()).isEqualTo(ReasonCode.GATE_NOT_FOUND);
    assertThat(f.turnstile.openCalls()).isZero();
  }

  // ---------- ADR-12 test 3: replay while online ----------

  @Test
  void should_reject_a_token_id_that_was_already_used_even_with_a_new_attempt_id() {
    f.token("t", GateFixture.E1, "jti-replay");
    assertThat(f.scan.scan(f.request("BD-GATE-01", "t")).outcome()).isEqualTo(Outcome.ALLOWED);

    var replay = f.scan.scan(f.request("BD-GATE-03", "t")); // a screenshot used at another gate

    assertThat(replay.outcome()).isEqualTo(Outcome.DENIED);
    assertThat(replay.reason()).isEqualTo(ReasonCode.QR_ALREADY_USED);
    assertThat(f.turnstile.openCalls()).isEqualTo(1);
  }

  // ---------- anti-passback ----------

  @Test
  void should_deny_two_consecutive_entries_at_an_entry_gate() {
    f.token("a", GateFixture.E1, "jti-a");
    f.token("b", GateFixture.E1, "jti-b");
    assertThat(f.scan.scan(f.request("BD-GATE-01", "a")).outcome()).isEqualTo(Outcome.ALLOWED);

    var second = f.scan.scan(f.request("BD-GATE-01", "b"));

    assertThat(second.reason()).isEqualTo(ReasonCode.ANTI_PASSBACK_VIOLATION);
    assertThat(f.turnstile.openCalls()).isEqualTo(1);
  }

  @Test
  void should_allow_entry_then_exit_through_two_different_gates() {
    f.token("a", GateFixture.E1, "jti-a");
    f.token("b", GateFixture.E1, "jti-b");

    assertThat(f.scan.scan(f.request("BD-GATE-01", "a")).direction()).isEqualTo(Direction.ENTRY);
    assertThat(f.scan.scan(f.request("BD-GATE-02", "b")).direction()).isEqualTo(Direction.EXIT);
  }

  @Test
  void should_alternate_direction_on_a_bidirectional_gate() {
    f.token("a", GateFixture.E1, "jti-a");
    f.token("b", GateFixture.E1, "jti-b");
    f.token("c", GateFixture.E1, "jti-c");

    assertThat(f.scan.scan(f.request("BD-GATE-03", "a")).direction()).isEqualTo(Direction.ENTRY);
    assertThat(f.scan.scan(f.request("BD-GATE-03", "b")).direction()).isEqualTo(Direction.EXIT);
    assertThat(f.scan.scan(f.request("BD-GATE-03", "c")).direction()).isEqualTo(Direction.ENTRY);
  }

  @Test
  void should_not_let_two_simultaneous_scans_of_one_employee_both_pass() {
    f.token("a", GateFixture.E1, "jti-a");
    f.token("b", GateFixture.E1, "jti-b");
    // the first scan is still pending (command sent, no answer yet)
    f.turnstile.setFault(Fault.TIMEOUT_NOT_OPENED);
    assertThat(f.scan.scan(f.request("BD-GATE-01", "a")).outcome()).isEqualTo(Outcome.PENDING);
    f.turnstile.setFault(Fault.NONE);

    var second = f.scan.scan(f.request("BD-GATE-03", "b"));

    assertThat(second.reason()).isEqualTo(ReasonCode.ANTI_PASSBACK_VIOLATION);
  }

  // ---------- ADR-11: retries and failures ----------

  // ADR-11 test 1 (P1/P5)
  @Test
  void should_answer_a_retry_from_the_stored_result_and_open_the_gate_only_once() {
    f.token("t", GateFixture.E1, "jti-1");
    var req = f.request("BD-GATE-01", "t");

    var first = f.scan.scan(req);
    var retry = f.scan.scan(req);

    assertThat(first.outcome()).isEqualTo(Outcome.ALLOWED);
    assertThat(retry.outcome()).isEqualTo(Outcome.ALLOWED);
    assertThat(retry.duplicate()).isTrue();
    assertThat(f.turnstile.openCalls()).isEqualTo(1);
  }

  // ADR-11 test 2 (P3): timeout never causes a second open command
  @Test
  void should_record_unknown_on_timeout_and_never_resend_the_open_command_on_retry() {
    f.token("t", GateFixture.E1, "jti-1");
    f.turnstile.setFault(
        Fault.TIMEOUT_BUT_OPENED); // the dangerous case: it opened, the ack was lost
    var req = f.request("BD-GATE-01", "t");

    var first = f.scan.scan(req);
    f.turnstile.setFault(Fault.NONE);
    var retry = f.scan.scan(req);

    assertThat(first.outcome()).isEqualTo(Outcome.PENDING);
    assertThat(first.reason()).isEqualTo(ReasonCode.TURNSTILE_TIMEOUT_RETRY_SCAN);
    assertThat(retry.outcome()).isEqualTo(Outcome.PENDING);
    assertThat(f.turnstile.openCalls()).isEqualTo(1);
    assertThat(f.attendance.state(req.attemptId()).orElseThrow().state())
        .isEqualTo(State.GATE_UNKNOWN);
  }

  @Test
  void should_keep_the_token_blocked_after_a_timeout_because_the_gate_may_have_opened() {
    f.token("t", GateFixture.E1, "jti-1");
    f.turnstile.setFault(Fault.TIMEOUT_BUT_OPENED);
    f.scan.scan(f.request("BD-GATE-01", "t"));
    f.turnstile.setFault(Fault.NONE);

    var again = f.scan.scan(f.request("BD-GATE-03", "t"));

    assertThat(again.reason()).isEqualTo(ReasonCode.QR_ALREADY_USED);
  }

  @Test
  void should_resolve_a_timeout_through_observation_without_a_new_open() {
    f.token("t", GateFixture.E1, "jti-1");
    f.turnstile.setFault(Fault.TIMEOUT_BUT_OPENED);
    var req = f.request("BD-GATE-01", "t");
    f.scan.scan(req);

    var observation = f.observe.observe(req.attemptId(), "BD-GATE-01");

    assertThat(observation.name()).isEqualTo("OPENED");
    assertThat(f.turnstile.openCalls()).isEqualTo(1);
  }

  @Test
  void should_report_undetermined_when_the_turnstile_cannot_tell() {
    f.token("t", GateFixture.E1, "jti-1");
    f.turnstile.setFault(Fault.TIMEOUT_BUT_OPENED);
    var req = f.request("BD-GATE-01", "t");
    f.scan.scan(req);
    f.turnstile.setStatusAvailable(false);

    assertThat(f.observe.observe(req.attemptId(), "BD-GATE-01").name()).isEqualTo("UNDETERMINED");
  }

  @Test
  void should_deny_and_release_the_token_on_a_hardware_error_so_the_person_can_rescan() {
    f.token("t", GateFixture.E1, "jti-1");
    f.turnstile.setFault(Fault.ERROR);
    var first = f.scan.scan(f.request("BD-GATE-01", "t"));
    f.turnstile.setFault(Fault.NONE);

    var second = f.scan.scan(f.request("BD-GATE-01", "t")); // same token, new attempt

    assertThat(first.outcome()).isEqualTo(Outcome.DENIED);
    assertThat(first.reason()).isEqualTo(ReasonCode.GATE_HARDWARE_ERROR);
    assertThat(second.outcome()).isEqualTo(Outcome.ALLOWED);
    assertThat(f.turnstile.openCalls()).isEqualTo(2);
  }

  @Test
  void should_release_everything_and_fail_when_attendance_is_down_before_the_command() {
    f.token("t", GateFixture.E1, "jti-1");
    f.attendance.failRecordAttempt(true);

    assertThatThrownBy(() -> f.scan.scan(f.request("BD-GATE-01", "t")))
        .isInstanceOf(IllegalStateException.class);
    f.attendance.failRecordAttempt(false);

    assertThat(f.turnstile.openCalls())
        .isZero(); // nothing physical happened without a durable record
    assertThat(f.scan.scan(f.request("BD-GATE-01", "t")).outcome()).isEqualTo(Outcome.ALLOWED);
  }

  // ADR-11 P4/P5: gate opened, record not saved yet
  @Test
  void should_answer_unconfirmed_when_the_gate_opened_but_the_result_could_not_be_saved() {
    f.token("t", GateFixture.E1, "jti-1");
    f.attendance.failRecordResult(true);

    var result = f.scan.scan(f.request("BD-GATE-01", "t"));

    assertThat(result.outcome()).isEqualTo(Outcome.UNCONFIRMED);
    assertThat(f.turnstile.openCalls()).isEqualTo(1);
  }

  @Test
  void should_report_in_progress_when_a_retry_arrives_while_the_first_request_is_unfinished() {
    f.token("t", GateFixture.E1, "jti-1");
    var req = f.request("BD-GATE-01", "t");
    // simulate the first request having stopped after the write-ahead marker
    f.attendance.recordAttempt(
        new vn.tmc.gatestation.application.port.out.IAttendanceGateway.AttemptRecord(
            req.attemptId(),
            GateFixture.E1,
            "BD-GATE-01",
            "BD",
            Direction.ENTRY,
            "jti-1",
            "ONLINE",
            f.clock.now(),
            "c"));
    f.attendance.markGateCommanded(req.attemptId());

    var retry = f.scan.scan(req);

    assertThat(retry.outcome()).isEqualTo(Outcome.IN_PROGRESS);
    assertThat(f.turnstile.openCalls()).isZero();
  }

  @Test
  void should_use_a_fresh_reservation_window_after_the_ttl_passes() {
    f.token("t", GateFixture.E1, "jti-1");
    assertThat(f.tokens.reserve("jti-1", Duration.ofSeconds(10))).isTrue();
    f.clock.advance(Duration.ofSeconds(11));

    assertThat(f.tokens.reserve("jti-1", Duration.ofSeconds(10))).isTrue();
  }
}
