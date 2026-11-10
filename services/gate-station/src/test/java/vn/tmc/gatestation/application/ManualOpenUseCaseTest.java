package vn.tmc.gatestation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.gatestation.application.port.in.IGateAccess.Outcome;
import vn.tmc.gatestation.application.port.in.IGateManualAccess.ManualOpenRequest;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.Trigger;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.support.GateFixture;
import vn.tmc.platform.kernel.ReasonCode;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;

class ManualOpenUseCaseTest {
  private final GateFixture f = new GateFixture();
  private final UUID guardId = UUID.randomUUID();
  private final Principal guard =
      new Principal(guardId, Set.of(Role.SECURITY_GUARD), DataScope.ofGates("BD", "BD-GATE-01"));

  private ManualOpenRequest request(String gate, UUID employee, String reason) {
    return new ManualOpenRequest(gate, employee, Direction.ENTRY, reason, "corr");
  }

  @Test
  void should_open_with_the_override_trigger_the_guard_id_and_an_audit_entry() {
    var result =
        f.manual.manualOpen(guard, request("BD-GATE-01", GateFixture.E1, "phone battery dead"));

    assertThat(result.outcome()).isEqualTo(Outcome.ALLOWED);
    assertThat(f.turnstile.openCalls()).isEqualTo(1);
    assertThat(f.turnstile.lastRequest().trigger()).isEqualTo(Trigger.MANUAL_GUARD_OVERRIDE);
    assertThat(f.turnstile.lastRequest().operatorId()).isEqualTo(guardId);
    assertThat(f.audit.entries())
        .anyMatch(
            e ->
                e.action().equals("MANUAL_OPEN_REQUESTED")
                    && e.detail().equals("phone battery dead"));
  }

  @Test
  void should_refuse_a_guard_who_is_not_assigned_to_the_gate() {
    assertThatThrownBy(
            () -> f.manual.manualOpen(guard, request("BD-GATE-02", GateFixture.E1, "reason")))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(f.turnstile.openCalls()).isZero();
  }

  @Test
  void should_refuse_a_caller_who_is_not_a_guard() {
    var hr = new Principal(UUID.randomUUID(), Set.of(Role.HR_STAFF), DataScope.all());

    assertThatThrownBy(
            () -> f.manual.manualOpen(hr, request("BD-GATE-01", GateFixture.E1, "reason")))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void should_require_a_reason() {
    assertThatThrownBy(
            () -> f.manual.manualOpen(guard, request("BD-GATE-01", GateFixture.E1, "  ")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(f.turnstile.openCalls()).isZero();
  }

  @Test
  void should_refuse_to_open_for_an_inactive_employee_and_audit_the_refusal() {
    var result = f.manual.manualOpen(guard, request("BD-GATE-01", GateFixture.E3, "forgot card"));

    assertThat(result.outcome()).isEqualTo(Outcome.DENIED);
    assertThat(result.reason()).isEqualTo(ReasonCode.EMPLOYEE_INACTIVE);
    assertThat(f.turnstile.openCalls()).isZero();
    assertThat(f.audit.entries()).anyMatch(e -> e.action().equals("MANUAL_OPEN_REFUSED"));
  }
}
