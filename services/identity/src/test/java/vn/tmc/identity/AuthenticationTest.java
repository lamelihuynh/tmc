package vn.tmc.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import vn.tmc.identity.adapter.out.inmem.InMemoryAccountRepository;
import vn.tmc.identity.adapter.out.inmem.Pbkdf2PasswordVerifier;
import vn.tmc.identity.application.port.in.IAuthentication.Status;
import vn.tmc.identity.application.usecase.AuthenticateUseCase;
import vn.tmc.identity.domain.Account;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.Role;

class AuthenticationTest {
  private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
  private final Pbkdf2PasswordVerifier passwords = new Pbkdf2PasswordVerifier();
  private Instant now = Instant.parse("2026-11-09T06:45:00Z");
  private final IClock clock = () -> now;
  private final AuthenticateUseCase auth =
      new AuthenticateUseCase(accounts, passwords, (u, code) -> "123456".equals(code), clock);

  private void account(String name, Role role) {
    accounts.save(new Account(UUID.randomUUID(), name, Set.of(role)));
    passwords.set(name, "correct-horse");
  }

  @Test
  void should_grant_a_supervisor_with_the_right_password_and_no_mfa() {
    account("sup", Role.PRODUCTION_SUPERVISOR);

    var r = auth.authenticate("sup", "correct-horse", null);

    assertThat(r.status()).isEqualTo(Status.GRANTED);
    assertThat(r.roles()).containsExactly(Role.PRODUCTION_SUPERVISOR);
  }

  @Test
  void should_not_reveal_whether_the_account_exists() {
    account("sup", Role.PRODUCTION_SUPERVISOR);

    assertThat(auth.authenticate("nobody", "x", null).status()).isEqualTo(Status.DENIED);
    assertThat(auth.authenticate("sup", "wrong", null).status()).isEqualTo(Status.DENIED);
  }

  // NFR-03
  @Test
  void should_require_mfa_for_hr_staff_and_system_admin() {
    account("hr", Role.HR_STAFF);
    account("admin", Role.SYSTEM_ADMIN);

    assertThat(auth.authenticate("hr", "correct-horse", null).status())
        .isEqualTo(Status.MFA_REQUIRED);
    assertThat(auth.authenticate("admin", "correct-horse", " ").status())
        .isEqualTo(Status.MFA_REQUIRED);
    assertThat(auth.authenticate("hr", "correct-horse", "123456").status())
        .isEqualTo(Status.GRANTED);
    assertThat(auth.authenticate("hr", "correct-horse", "000000").status())
        .isEqualTo(Status.DENIED);
  }

  // NFR-03
  @Test
  void should_lock_the_account_after_five_wrong_passwords_even_for_the_right_one() {
    account("sup", Role.PRODUCTION_SUPERVISOR);

    for (int i = 0; i < 4; i++) {
      assertThat(auth.authenticate("sup", "bad", null).status()).isEqualTo(Status.DENIED);
    }
    assertThat(auth.authenticate("sup", "bad", null).status()).isEqualTo(Status.LOCKED);

    assertThat(auth.authenticate("sup", "correct-horse", null).status()).isEqualTo(Status.LOCKED);
  }

  @Test
  void should_unlock_after_the_lock_time() {
    account("sup", Role.PRODUCTION_SUPERVISOR);
    for (int i = 0; i < 5; i++) {
      auth.authenticate("sup", "bad", null);
    }

    now = now.plus(Account.LOCK_TIME).plusSeconds(1);

    assertThat(auth.authenticate("sup", "correct-horse", null).status()).isEqualTo(Status.GRANTED);
  }

  @Test
  void should_reset_the_failure_counter_after_a_success() {
    account("sup", Role.PRODUCTION_SUPERVISOR);
    for (int i = 0; i < 4; i++) {
      auth.authenticate("sup", "bad", null);
    }
    auth.authenticate("sup", "correct-horse", null);

    for (int i = 0; i < 4; i++) {
      assertThat(auth.authenticate("sup", "bad", null).status()).isEqualTo(Status.DENIED);
    }
  }

  @Test
  void should_deny_a_deactivated_account() {
    account("sup", Role.PRODUCTION_SUPERVISOR);
    accounts.findByUsername("sup").orElseThrow().deactivate();

    assertThat(auth.authenticate("sup", "correct-horse", null).status()).isEqualTo(Status.DENIED);
  }

  @Test
  void should_not_keep_passwords_in_clear_text() {
    passwords.set("x", "secret-123");

    assertThat(passwords.matches("x", "secret-123")).isTrue();
    assertThat(passwords.toString()).doesNotContain("secret-123");
  }

  @Disabled(
      "TODO Team C (ADR-08): issue an ES256 JWT with roles and scope claims and serve the JWKS")
  @Test
  void should_issue_a_signed_token_on_successful_login() {}

  @Disabled("TODO Team C (NFR-03): sessions expire after 15 minutes of inactivity")
  @Test
  void should_expire_an_idle_session() {}

  @Disabled(
      "TODO Team C (FR-31): create, change role, deactivate accounts; role change ends open sessions")
  @Test
  void should_end_open_sessions_when_a_role_changes() {}

  @Disabled(
      "TODO Team C (FR-08): employee eligibility feed for Gate Station through the sync bundle")
  @Test
  void should_publish_employee_eligibility_for_the_edge() {}
}
