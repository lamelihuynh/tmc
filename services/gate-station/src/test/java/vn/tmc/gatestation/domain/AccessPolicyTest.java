package vn.tmc.gatestation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.gatestation.domain.policy.AccessContext;
import vn.tmc.gatestation.domain.policy.AccessPolicy;
import vn.tmc.gatestation.domain.policy.AccessPolicyChain;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * ADR-13 compliance 3: a new rule is added as a new class/lambda, the chain and use case stay
 * untouched.
 */
class AccessPolicyTest {
  private final Instant now = Instant.parse("2026-11-09T06:45:00Z");
  private final GateConfig gate =
      new GateConfig("BD-GATE-01", "BD", "HW", GateMode.ENTRY_ONLY, false);
  private final EmployeeAccess employee = new EmployeeAccess(UUID.randomUUID(), true, Set.of("BD"));

  private AccessContext context() {
    return new AccessContext(employee, gate, Direction.ENTRY, Optional.empty(), now);
  }

  @Test
  void should_pass_when_every_rule_agrees() {
    assertThat(AccessPolicyChain.standard().evaluate(context())).isEmpty();
  }

  @Test
  void should_let_a_new_rule_deny_without_changing_existing_ones() {
    AccessPolicy shiftWindow = ctx -> Optional.of(ReasonCode.GATE_DIRECTION_MISMATCH);
    var chain =
        new AccessPolicyChain(
            List.of(
                new vn.tmc.gatestation.domain.policy.ActiveEmployeeRule(),
                new vn.tmc.gatestation.domain.policy.FactoryPermissionRule(),
                shiftWindow));

    assertThat(chain.evaluate(context())).contains(ReasonCode.GATE_DIRECTION_MISMATCH);
  }

  @Test
  void should_stop_at_the_first_denial() {
    var calls = new int[1];
    AccessPolicy counting =
        ctx -> {
          calls[0]++;
          return Optional.empty();
        };
    var inactive = new EmployeeAccess(employee.employeeUuid(), false, Set.of("BD"));
    var chain =
        new AccessPolicyChain(
            List.of(new vn.tmc.gatestation.domain.policy.ActiveEmployeeRule(), counting));

    var denial =
        chain.evaluate(new AccessContext(inactive, gate, Direction.ENTRY, Optional.empty(), now));

    assertThat(denial).contains(ReasonCode.EMPLOYEE_INACTIVE);
    assertThat(calls[0]).isZero();
  }

  @Test
  void should_resolve_direction_from_gate_mode_and_last_pass() {
    var bidi = new GateConfig("G", "BD", "HW", GateMode.BIDIRECTIONAL, false);

    assertThat(DirectionResolver.resolve(bidi, Optional.empty())).isEqualTo(Direction.ENTRY);
    assertThat(DirectionResolver.resolve(bidi, Optional.of(new LastPass(Direction.ENTRY, now))))
        .isEqualTo(Direction.EXIT);
    assertThat(DirectionResolver.resolve(gate, Optional.of(new LastPass(Direction.ENTRY, now))))
        .isEqualTo(Direction.ENTRY);
  }
}
