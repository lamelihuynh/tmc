package vn.tmc.identity.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.identity.adapter.out.crypto.SigningKeys;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.AccessTokenSigner;
import vn.tmc.platform.security.jwt.JwtPrincipalVerifier;
import vn.tmc.platform.security.jwt.TestJwtKeys;

/**
 * C-12 (NFR-03, identity.openapi.yaml JWKS): changing the signing key keeps tokens of the old key
 * valid while the old public key is still published, and only then.
 */
class SigningKeyRotationTest {
  private final Instant now = Instant.parse("2026-11-20T08:00:00Z");
  private final IClock clock = () -> now;
  private final Principal supervisor =
      new Principal(UUID.randomUUID(), Set.of(Role.PRODUCTION_SUPERVISOR), DataScope.all());

  private final AccessTokenSigner oldKey = TestJwtKeys.TRUSTED; // kid test-key-a
  private final AccessTokenSigner newKey = AccessTokenSigner.ephemeral();

  private JwtPrincipalVerifier verifierOf(SigningKeys keys) {
    return JwtPrincipalVerifier.fromJwksJson(keys.publicJwksJson(), clock);
  }

  @Test
  void should_accept_an_old_token_still_valid_after_the_key_changed() {
    var oldToken = oldKey.sign(supervisor, now.minusSeconds(300), "jti-old");

    var rotated = verifierOf(SigningKeys.of(newKey, oldKey.publicJwk().toJSONString()));

    assertThat(rotated.verify(oldToken)).isNotNull();
    assertThat(rotated.verify(newKey.sign(supervisor, now, "jti-new")))
        .extracting(Principal::subject)
        .isEqualTo(supervisor.subject());
  }

  @Test
  void should_publish_both_keys_during_the_rotation_and_no_private_part() throws Exception {
    var keys = SigningKeys.of(newKey, TestJwtKeys.TRUSTED_PRIVATE_JWK); // private given by mistake

    var jwks = JWKSet.parse(keys.publicJwksJson());

    assertThat(jwks.getKeys())
        .extracting(k -> k.getKeyID())
        .containsExactly(newKey.keyId(), "test-key-a");
    assertThat(jwks.getKeys()).noneMatch(k -> k.isPrivate());
    assertThat(keys.publicJwksJson()).doesNotContain("\"d\"");
    assertThat(keys.current()).isSameAs(newKey);
  }

  @Test
  void should_refuse_the_old_token_once_the_old_key_is_removed() {
    var oldToken = oldKey.sign(supervisor, now.minusSeconds(300), "jti-old");

    var afterRotation = verifierOf(SigningKeys.of(newKey, ""));

    assertThat(afterRotation.verify(oldToken)).isNull();
    assertThat(afterRotation.verify(newKey.sign(supervisor, now, "jti-new"))).isNotNull();
  }

  @Test
  void should_reject_a_previous_key_that_cannot_be_told_apart_or_is_not_es256() throws Exception {
    var sameKid = newKey.publicJwk().toJSONString();
    var p384 = new ECKeyGenerator(Curve.P_384).keyID("old-p384").generate().toJSONString();

    assertThatThrownBy(() -> SigningKeys.of(newKey, sameKid))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("same kid");
    assertThatThrownBy(() -> SigningKeys.of(newKey, p384))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("P-256");
    assertThatThrownBy(() -> SigningKeys.of(newKey, "{not json"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
