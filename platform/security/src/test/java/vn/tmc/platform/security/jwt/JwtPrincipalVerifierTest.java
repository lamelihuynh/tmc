package vn.tmc.platform.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.PrincipalResolver;
import vn.tmc.platform.security.Role;

/** The verification rules of docs/api/identity.openapi.yaml, one test per rule. */
class JwtPrincipalVerifierTest {
  private final UUID subject = UUID.fromString("0192a1c0-0000-7000-8000-000000000001");
  private final Principal supervisor =
      new Principal(
          subject,
          Set.of(Role.PRODUCTION_SUPERVISOR),
          new DataScope(Set.of("BD"), Set.of(), Set.of("BD-GATE-01"), false));
  private final JwtPrincipalVerifier verifier = TestJwtKeys.verifier();

  @Test
  void should_turn_a_valid_token_into_the_same_principal() {
    var p = verifier.verify(TestTokens.valid(supervisor));

    assertThat(p).isNotNull();
    assertThat(p.subject()).isEqualTo(subject);
    assertThat(p.roles()).containsExactly(Role.PRODUCTION_SUPERVISOR);
    assertThat(p.scope().factories()).containsExactly("BD");
    assertThat(p.scope().gates()).containsExactly("BD-GATE-01");
    assertThat(p.scope().allFactories()).isFalse();
  }

  @Test
  void should_map_the_star_factory_to_all_factories() {
    var admin = new Principal(subject, Set.of(Role.SYSTEM_ADMIN), DataScope.all());

    assertThat(verifier.verify(TestTokens.valid(admin)).scope().allFactories()).isTrue();
  }

  @Test
  void should_refuse_expired_foreign_unsigned_and_tampered_tokens() {
    assertThat(verifier.verify(TestTokens.expired(supervisor))).isNull();
    assertThat(verifier.verify(TestTokens.signedByUntrustedKey(supervisor))).isNull();
    assertThat(verifier.verify(TestTokens.unsigned(supervisor))).isNull();
    assertThat(verifier.verify(TestTokens.withIssuer(supervisor, "someone-else"))).isNull();
    assertThat(verifier.verify(TestTokens.withAudience(supervisor, "other-api"))).isNull();
    assertThat(verifier.verify(TestTokens.withRoles(supervisor, List.of("EMPLOYEE", "ROOT"))))
        .isNull();
    assertThat(verifier.verify("not.a.jwt")).isNull();
    assertThat(verifier.verify(null)).isNull();
  }

  @Test
  void should_allow_thirty_seconds_of_clock_skew_and_no_more() {
    var issued = Instant.parse("2026-11-20T08:00:00Z");
    var token = TestJwtKeys.TRUSTED.sign(supervisor, issued, "jti-1");
    var expiry = issued.plus(AccessTokenContract.TTL);

    assertThat(TestJwtKeys.verifier(() -> expiry.plusSeconds(29)).verify(token)).isNotNull();
    assertThat(TestJwtKeys.verifier(() -> expiry.plusSeconds(30)).verify(token)).isNull();
    // issued in the future (a server clock far ahead) is refused
    assertThat(TestJwtKeys.verifier(() -> issued.minus(Duration.ofSeconds(31))).verify(token))
        .isNull();
  }

  @Test
  void should_publish_a_jwks_without_the_private_part() {
    assertThat(TestJwtKeys.trustedJwksJson())
        .contains("\"kid\":\"test-key-a\"")
        .doesNotContain("\"d\"");
  }

  @Test
  void should_never_fall_back_to_dev_headers_when_a_bearer_token_is_present() {
    var resolver = new PrincipalResolver(verifier, true);
    var bad = TestTokens.signedByUntrustedKey(supervisor);
    Function<String, String> headers =
        h ->
            switch (h) {
              case "Authorization" -> TestTokens.bearer(bad);
              case "X-Subject" -> subject.toString();
              case "X-Roles" -> "SYSTEM_ADMIN";
              default -> null;
            };

    assertThat(resolver.resolve(headers)).isNull();
  }

  @Test
  void should_use_dev_headers_only_when_allowed_and_no_authorization_is_sent() {
    Function<String, String> devHeaders =
        h ->
            switch (h) {
              case "X-Subject" -> subject.toString();
              case "X-Roles" -> "EMPLOYEE";
              default -> null;
            };

    assertThat(new PrincipalResolver(verifier, true).resolve(devHeaders)).isNotNull();
    assertThat(new PrincipalResolver(verifier, false).resolve(devHeaders)).isNull();
    assertThat(
            new PrincipalResolver(verifier, false)
                .resolve(
                    h ->
                        "Authorization".equals(h)
                            ? TestTokens.bearer(TestTokens.valid(supervisor))
                            : null)
                .subject())
        .isEqualTo(subject);
    // no verifier configured: a Bearer token cannot be checked, so it is refused
    assertThat(
            PrincipalResolver.devHeadersOnly()
                .resolve(
                    h ->
                        "Authorization".equals(h)
                            ? TestTokens.bearer(TestTokens.valid(supervisor))
                            : null))
        .isNull();
  }
}
