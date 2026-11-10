package vn.tmc.platform.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RbacTest {
  private final Principal guard =
      new Principal(
          UUID.randomUUID(), Set.of(Role.SECURITY_GUARD), DataScope.ofGates("BD", "BD-GATE-01"));

  @Test
  void should_allow_guard_on_assigned_gate() {
    assertThatCode(() -> Rbac.requireGate(guard, "BD-GATE-01")).doesNotThrowAnyException();
  }

  @Test
  void should_deny_guard_on_other_gate() {
    assertThatThrownBy(() -> Rbac.requireGate(guard, "BD-GATE-02"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void should_deny_wrong_role() {
    assertThatThrownBy(() -> Rbac.requireRole(guard, Role.HR_STAFF, Role.SYSTEM_ADMIN))
        .isInstanceOf(AccessDeniedException.class);
  }
}
