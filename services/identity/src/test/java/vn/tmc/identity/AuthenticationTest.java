package vn.tmc.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.tmc.identity.application.usecase.AuthenticateUseCase.LOGIN_FAILED;
import static vn.tmc.identity.application.usecase.AuthenticateUseCase.LOGIN_LOCKED;
import static vn.tmc.identity.application.usecase.AuthenticateUseCase.LOGIN_MFA_REQUIRED;
import static vn.tmc.identity.application.usecase.AuthenticateUseCase.LOGIN_SUCCEEDED;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import vn.tmc.identity.adapter.out.crypto.NimbusAccessTokenIssuer;
import vn.tmc.identity.adapter.out.inmem.InMemoryAccountRepository;
import vn.tmc.identity.adapter.out.inmem.Pbkdf2PasswordVerifier;
import vn.tmc.identity.adapter.out.messaging.OutboxAuditLog;
import vn.tmc.identity.application.port.in.IAuthentication.Status;
import vn.tmc.identity.application.usecase.AuthenticateUseCase;
import vn.tmc.identity.application.usecase.LoginUseCase;
import vn.tmc.identity.domain.Account;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.messaging.TmcExchange;
import vn.tmc.platform.messaging.outbox.AuditOutbox;
import vn.tmc.platform.messaging.outbox.InMemoryOutboxStore;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.AccessTokenContract;
import vn.tmc.platform.security.jwt.TestJwtKeys;

class AuthenticationTest {
  private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
  private final Pbkdf2PasswordVerifier passwords = new Pbkdf2PasswordVerifier();
  private Instant now = Instant.parse("2026-11-09T06:45:00Z");
  private final IClock clock = () -> now;
  private final InMemoryOutboxStore outbox = new InMemoryOutboxStore();
  private final AuthenticateUseCase auth =
      new AuthenticateUseCase(
          accounts,
          passwords,
          (u, code) -> "123456".equals(code),
          new OutboxAuditLog(new AuditOutbox(outbox, TmcExchange.IDENTITY, clock)),
          clock);

  private UUID account(String name, Role role) {
    var subject = UUID.randomUUID();
    accounts.save(new Account(subject, name, Set.of(role)));
    passwords.set(name, "correct-horse");
    return subject;
  }

  private List<EventEnvelope> audit() {
    return outbox.fetchPending(1000);
  }

  private static String action(EventEnvelope e) {
    return (String) e.payload().get("action");
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

  @Test
  void should_issue_a_signed_token_on_successful_login() {
    accounts.save(
        new Account(UUID.randomUUID(), "sup", Set.of(Role.PRODUCTION_SUPERVISOR), Set.of("BD")));
    passwords.set("sup", "correct-horse");
    now = Instant.now(); // the shared test verifier runs on the system clock
    var login = new LoginUseCase(auth, new NimbusAccessTokenIssuer(TestJwtKeys.TRUSTED), clock);

    var granted = login.login("sup", "correct-horse", null);
    var refused = login.login("sup", "wrong", null);

    var principal = TestJwtKeys.verifier().verify(granted.accessToken());
    assertThat(principal.subject()).isEqualTo(granted.subject());
    assertThat(principal.roles()).containsExactly(Role.PRODUCTION_SUPERVISOR);
    assertThat(principal.scope().factories()).containsExactly("BD");
    assertThat(granted.expiresAt()).isEqualTo(now.plus(AccessTokenContract.TTL));
    assertThat(refused.accessToken()).isNull();
    assertThat(refused.subject()).isNull();
  }

  // C-08
  @Test
  void should_emit_a_lock_event_after_five_wrong_passwords_without_any_secret() throws Exception {
    var subject = account("sup", Role.PRODUCTION_SUPERVISOR);

    for (int i = 0; i < 5; i++) {
      auth.authenticate("sup", "wrong-pass-" + i, null);
    }

    assertThat(audit())
        .extracting(AuthenticationTest::action)
        .containsExactly(
            LOGIN_FAILED, LOGIN_FAILED, LOGIN_FAILED, LOGIN_FAILED, LOGIN_FAILED, LOGIN_LOCKED);
    var locked = audit().get(5);
    assertThat(locked.eventType()).isEqualTo("AuditRecorded");
    assertThat(locked.source()).isEqualTo(TmcExchange.IDENTITY);
    assertThat(locked.payload())
        .containsEntry("target", subject.toString())
        .containsEntry("result", "LOCKED")
        .doesNotContainKey("actor");
    var json = new ObjectMapper().findAndRegisterModules().writeValueAsString(audit());
    assertThat(json).doesNotContain("wrong-pass", "correct-horse", "\"sup\"");
  }

  // C-08
  @Test
  void should_audit_success_mfa_challenge_and_unknown_account_with_reason_codes_only() {
    var sup = account("sup", Role.PRODUCTION_SUPERVISOR);
    account("hr", Role.HR_STAFF);

    auth.authenticate("sup", "correct-horse", null);
    auth.authenticate("hr", "correct-horse", null);
    auth.authenticate("hr", "correct-horse", "000000");
    auth.authenticate("nobody", "x", null);

    assertThat(audit())
        .extracting(e -> action(e) + ":" + e.payload().get("result"))
        .containsExactly(
            LOGIN_SUCCEEDED + ":OK",
            LOGIN_MFA_REQUIRED + ":MFA_REQUIRED",
            LOGIN_FAILED + ":" + AuthenticateUseCase.BAD_MFA_CODE,
            LOGIN_FAILED + ":" + AuthenticateUseCase.UNKNOWN_ACCOUNT);
    assertThat(audit().get(0).payload()).containsEntry("actor", sup.toString());
    assertThat(audit().get(3).payload()).doesNotContainKeys("target", "actor");
  }

  // C-08
  @Test
  void should_audit_an_attempt_on_a_locked_account() {
    account("sup", Role.PRODUCTION_SUPERVISOR);
    for (int i = 0; i < 5; i++) {
      auth.authenticate("sup", "bad", null);
    }

    auth.authenticate("sup", "correct-horse", null);

    assertThat(audit().get(audit().size() - 1).payload())
        .containsEntry("action", LOGIN_FAILED)
        .containsEntry("result", AuthenticateUseCase.ACCOUNT_LOCKED);
  }

  @Disabled("TODO Team C (NFR-03): sessions expire after 15 minutes of inactivity")
  @Test
  void should_expire_an_idle_session() {}

  @Disabled(
      "TODO Team C (FR-31): create, change role, deactivate accounts; role change ends open sessions")
  @Test
  void should_end_open_sessions_when_a_role_changes() {}
}
