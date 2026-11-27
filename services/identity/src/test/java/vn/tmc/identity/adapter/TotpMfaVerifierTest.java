package vn.tmc.identity.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.identity.adapter.out.crypto.TotpMfaVerifier;
import vn.tmc.identity.adapter.out.inmem.InMemoryAccountRepository;
import vn.tmc.identity.adapter.out.inmem.Pbkdf2PasswordVerifier;
import vn.tmc.identity.application.port.in.IAuthentication.Status;
import vn.tmc.identity.application.usecase.AuthenticateUseCase;
import vn.tmc.identity.domain.Account;
import vn.tmc.platform.security.Role;

/**
 * C-05: RFC 6238 TOTP verification — valid code, wrong code, ±1 step window, replay rejection,
 * standard RFC 6238 vectors, and drop-in wiring into AuthenticateUseCase without changing it.
 */
class TotpMfaVerifierTest {
  private static final byte[] RFC6238_SHA1_SECRET =
      "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
  private static final String BASE32_SECRET = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

  private Instant now = Instant.parse("2026-11-19T08:00:00Z");

  private TotpMfaVerifier verifier() {
    var v = new TotpMfaVerifier(() -> now);
    v.registerBase32("hr1", BASE32_SECRET);
    v.registerBase32("admin1", BASE32_SECRET);
    return v;
  }

  @Test
  void should_match_rfc_6238_sha1_test_vectors() {
    // RFC 6238 Appendix B (last 6 digits of the 8-digit SHA-1 test vectors)
    assertThat(
            TotpMfaVerifier.generateCode(
                RFC6238_SHA1_SECRET, TotpMfaVerifier.timeStep(Instant.ofEpochSecond(59L))))
        .isEqualTo("287082");
    assertThat(
            TotpMfaVerifier.generateCode(
                RFC6238_SHA1_SECRET, TotpMfaVerifier.timeStep(Instant.ofEpochSecond(1111111109L))))
        .isEqualTo("081804");
    assertThat(
            TotpMfaVerifier.generateCode(
                RFC6238_SHA1_SECRET, TotpMfaVerifier.timeStep(Instant.ofEpochSecond(1234567890L))))
        .isEqualTo("005924");
  }

  @Test
  void should_accept_the_current_step_code_and_reject_wrong_or_malformed_codes() {
    var v = verifier();
    var valid = TotpMfaVerifier.generateCodeAt(BASE32_SECRET, now);

    assertThat(v.verify("hr1", "000000".equals(valid) ? "111111" : "000000")).isFalse();
    assertThat(v.verify("hr1", "12345")).isFalse();
    assertThat(v.verify("hr1", "1234567")).isFalse();
    assertThat(v.verify("hr1", "12ab56")).isFalse();
    assertThat(v.verify("hr1", null)).isFalse();
    assertThat(v.verify("unknown", valid)).isFalse();
    assertThat(v.verify("hr1", valid)).isTrue();
  }

  @Test
  void should_accept_previous_and_next_time_steps_but_reject_two_steps_away() {
    var prevStep = TotpMfaVerifier.generateCodeAt(BASE32_SECRET, now.minusSeconds(30));
    var nextStep = TotpMfaVerifier.generateCodeAt(BASE32_SECRET, now.plusSeconds(30));
    var tooOld = TotpMfaVerifier.generateCodeAt(BASE32_SECRET, now.minusSeconds(60));
    var tooNew = TotpMfaVerifier.generateCodeAt(BASE32_SECRET, now.plusSeconds(60));

    var v1 = verifier();
    assertThat(v1.verify("hr1", tooOld)).isFalse();
    assertThat(v1.verify("hr1", tooNew)).isFalse();
    assertThat(v1.verify("hr1", prevStep)).isTrue();

    var v2 = verifier();
    assertThat(v2.verify("admin1", nextStep)).isTrue();
  }

  @Test
  void should_reject_replaying_a_code_that_was_already_used() {
    var v = verifier();
    var code = TotpMfaVerifier.generateCodeAt(BASE32_SECRET, now);

    assertThat(v.verify("hr1", code)).isTrue();
    // same step cannot be accepted a second time
    assertThat(v.verify("hr1", code)).isFalse();
    // nor can an older step after the current step was already consumed
    var previous = TotpMfaVerifier.generateCodeAt(BASE32_SECRET, now.minusSeconds(30));
    assertThat(v.verify("hr1", previous)).isFalse();
    // another user is tracked independently
    assertThat(v.verify("admin1", code)).isTrue();

    // once the clock moves into the next 30-second window, a fresh code succeeds
    now = now.plusSeconds(30);
    var nextCode = TotpMfaVerifier.generateCodeAt(BASE32_SECRET, now);
    assertThat(v.verify("hr1", nextCode)).isTrue();
  }

  @Test
  void should_never_leak_secrets_in_toString() {
    var v = verifier();
    assertThat(v.toString()).doesNotContain(BASE32_SECRET);
    assertThatThrownBy(() -> TotpMfaVerifier.decodeBase32("A"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_work_with_authenticate_use_case_for_hr_staff_and_system_admin() {
    var accounts = new InMemoryAccountRepository();
    var passwords = new Pbkdf2PasswordVerifier();
    accounts.save(new Account(UUID.randomUUID(), "hr1", Set.of(Role.HR_STAFF)));
    accounts.save(new Account(UUID.randomUUID(), "admin1", Set.of(Role.SYSTEM_ADMIN)));
    passwords.set("hr1", "correct-horse");
    passwords.set("admin1", "correct-horse");

    var totp = verifier();
    var auth = new AuthenticateUseCase(accounts, passwords, totp, (a, act, t, r) -> {}, () -> now);

    var hrCode = TotpMfaVerifier.generateCodeAt(BASE32_SECRET, now);
    assertThat(auth.authenticate("hr1", "correct-horse", null).status())
        .isEqualTo(Status.MFA_REQUIRED);
    assertThat(auth.authenticate("hr1", "correct-horse", hrCode).status())
        .isEqualTo(Status.GRANTED);
    assertThat(auth.authenticate("hr1", "correct-horse", hrCode).status()).isEqualTo(Status.DENIED);

    var adminNextStepCode = TotpMfaVerifier.generateCodeAt(BASE32_SECRET, now.plusSeconds(30));
    assertThat(auth.authenticate("admin1", "correct-horse", adminNextStepCode).status())
        .isEqualTo(Status.GRANTED);
  }
}
