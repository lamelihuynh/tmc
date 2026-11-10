package vn.tmc.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrincipalHeadersTest {
  private final String id = UUID.randomUUID().toString();

  @Test
  void should_parse_roles_and_gate_scope() {
    var p = PrincipalHeaders.parse(id, "SECURITY_GUARD", "BD", "BD-GATE-01,BD-GATE-02");

    assertThat(p.roles()).containsExactly(Role.SECURITY_GUARD);
    assertThat(p.scope().coversGate("BD-GATE-02")).isTrue();
    assertThat(p.scope().coversGate("LA-GATE-01")).isFalse();
  }

  @Test
  void should_give_all_scope_for_a_star() {
    assertThat(PrincipalHeaders.parse(id, "HR_STAFF", "*", null).scope().coversFactory("LA"))
        .isTrue();
  }

  @Test
  void should_treat_missing_or_malformed_headers_as_unauthenticated() {
    assertThat(PrincipalHeaders.parse(null, "HR_STAFF", null, null)).isNull();
    assertThat(PrincipalHeaders.parse(id, null, null, null)).isNull();
    assertThat(PrincipalHeaders.parse("not-a-uuid", "HR_STAFF", null, null)).isNull();
    assertThat(PrincipalHeaders.parse(id, "GOD_MODE", null, null)).isNull();
  }
}
