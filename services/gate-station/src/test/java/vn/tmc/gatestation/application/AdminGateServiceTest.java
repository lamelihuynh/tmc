package vn.tmc.gatestation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.gatestation.application.port.in.IGateAccess.Outcome;
import vn.tmc.gatestation.application.usecase.AdminGateService;
import vn.tmc.gatestation.domain.GateConfig;
import vn.tmc.gatestation.domain.GateMode;
import vn.tmc.gatestation.support.GateFixture;
import vn.tmc.platform.kernel.ReasonCode;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;

class AdminGateServiceTest {
  private final GateFixture f = new GateFixture();
  private final AdminGateService admin = new AdminGateService(f.gates, f.audit);
  private final Principal systemAdmin =
      new Principal(UUID.randomUUID(), Set.of(Role.SYSTEM_ADMIN), DataScope.all());

  @Test
  void should_refuse_every_operation_for_a_caller_who_is_not_system_admin() {
    var guard =
        new Principal(
            UUID.randomUUID(), Set.of(Role.SECURITY_GUARD), DataScope.ofGates("BD", "BD-GATE-01"));
    var gate = new GateConfig("BD-GATE-09", "BD", "BD-HW-09", GateMode.ENTRY_ONLY, false);

    assertThatThrownBy(() -> admin.list(guard)).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> admin.find(guard, "BD-GATE-01"))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> admin.upsert(guard, gate)).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> admin.setMaintenance(guard, "BD-GATE-01", true))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(f.gates.find("BD-GATE-09")).isEmpty();
  }

  @Test
  void should_upsert_a_gate_then_read_it_back() {
    var gate = new GateConfig("BD-GATE-09", "BD", "BD-HW-09", GateMode.BIDIRECTIONAL, false);

    assertThat(admin.upsert(systemAdmin, gate)).isEqualTo(gate);
    assertThat(admin.find(systemAdmin, "BD-GATE-09")).contains(gate);
    assertThat(admin.list(systemAdmin)).contains(gate);

    var changed = new GateConfig("BD-GATE-09", "BD", "BD-HW-09", GateMode.EXIT_ONLY, true);
    assertThat(admin.upsert(systemAdmin, changed)).isEqualTo(changed);
    assertThat(admin.find(systemAdmin, "BD-GATE-09")).contains(changed);
  }

  @Test
  void should_make_maintenance_effective_on_the_next_scan() {
    assertThat(admin.setMaintenance(systemAdmin, "BD-GATE-01", true)).isPresent();
    assertThat(f.gates.find("BD-GATE-01").orElseThrow().maintenance()).isTrue();

    var result = f.scan.scan(f.request("BD-GATE-01", "any-token"));

    assertThat(result.outcome()).isEqualTo(Outcome.DENIED);
    assertThat(result.reason()).isEqualTo(ReasonCode.GATE_IN_MAINTENANCE);
    assertThat(f.turnstile.openCalls()).isZero();
  }

  @Test
  void should_audit_upsert_and_maintenance_changes() {
    admin.upsert(
        systemAdmin, new GateConfig("BD-GATE-09", "BD", "BD-HW-09", GateMode.ENTRY_ONLY, false));
    admin.setMaintenance(systemAdmin, "BD-GATE-09", true);

    assertThat(f.audit.entries())
        .anyMatch(
            e ->
                e.action().equals("GATE_UPSERTED")
                    && e.target().equals("BD-GATE-09")
                    && e.detail().contains("factoryCode=BD")
                    && e.detail().contains("hardwareGateId=BD-HW-09")
                    && e.detail().contains("mode=ENTRY_ONLY"))
        .anyMatch(
            e ->
                e.action().equals("GATE_MAINTENANCE_CHANGED")
                    && e.target().equals("BD-GATE-09")
                    && e.detail().contains("maintenance=true"));
  }

  @Test
  void should_reject_a_gate_without_required_fields() {
    assertThatThrownBy(
            () ->
                admin.upsert(
                    systemAdmin, new GateConfig("", "BD", "BD-HW-09", GateMode.ENTRY_ONLY, false)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(f.gates.find("")).isEmpty();
  }
}
