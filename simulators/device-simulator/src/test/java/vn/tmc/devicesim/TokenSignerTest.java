package vn.tmc.devicesim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TokenSignerTest {
  private final UUID employee = UUID.randomUUID();

  private DeviceProfile registered() {
    return DeviceProfile.generate(employee).withDeviceId("dev-test-1");
  }

  private boolean verifies(String token, DeviceProfile profile) throws Exception {
    var pub =
        (ECPublicKey)
            KeyFactory.getInstance("EC")
                .generatePublic(
                    new X509EncodedKeySpec(
                        Base64.getDecoder().decode(profile.publicKeyX509Base64())));
    return SignedJWT.parse(token).verify(new ECDSAVerifier(pub));
  }

  @Test
  void should_sign_a_token_the_registered_public_key_verifies() throws Exception {
    var profile = registered();

    var token = TokenSigner.sign(profile, List.of("BD"), TokenSigner.Options.defaults());

    assertThat(verifies(token, profile)).isTrue();
    var jwt = SignedJWT.parse(token);
    assertThat(jwt.getHeader().getKeyID()).isEqualTo("dev-test-1");
    assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(employee.toString());
  }

  @Test
  void should_not_verify_under_another_devices_key() throws Exception {
    var token = TokenSigner.sign(registered(), List.of("BD"), TokenSigner.Options.defaults());

    assertThat(verifies(token, DeviceProfile.generate(employee).withDeviceId("dev-other")))
        .isFalse();
  }

  @Test
  void should_use_a_short_lifetime_and_only_the_allowed_claims_by_default() throws Exception {
    var token = TokenSigner.sign(registered(), List.of("BD"), TokenSigner.Options.defaults());
    var claims = SignedJWT.parse(token).getJWTClaimsSet();

    assertThat(claims.getExpirationTime().getTime() - claims.getIssueTime().getTime())
        .isEqualTo(30_000);
    assertThat(claims.getClaims().keySet())
        .isSubsetOf(Set.of("sub", "jti", "iat", "exp", "fac", "nonce"));
  }

  @Test
  void should_offer_deliberately_bad_tokens_for_demonstrations() throws Exception {
    var pii =
        TokenSigner.sign(
            registered(),
            List.of("BD"),
            new TokenSigner.Options(30, null, true, false, Instant.now()));
    var long24h =
        TokenSigner.sign(
            registered(),
            List.of("BD"),
            new TokenSigner.Options(30, null, false, true, Instant.now()));

    assertThat(SignedJWT.parse(pii).getJWTClaimsSet().getClaims()).containsKey("name");
    var c = SignedJWT.parse(long24h).getJWTClaimsSet();
    assertThat(c.getExpirationTime().getTime() - c.getIssueTime().getTime())
        .isEqualTo(24 * 3600 * 1000L);
  }

  @Test
  void should_refuse_to_sign_before_the_device_is_registered() {
    assertThatThrownBy(
            () ->
                TokenSigner.sign(
                    DeviceProfile.generate(employee),
                    List.of("BD"),
                    TokenSigner.Options.defaults()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void should_round_trip_the_profile_through_a_file(@TempDir Path dir) throws Exception {
    var profile = registered();
    var file = dir.resolve("emp.properties");

    profile.save(file);
    var loaded = DeviceProfile.load(file);

    assertThat(loaded).isEqualTo(profile);
    assertThat(
            verifies(
                TokenSigner.sign(loaded, List.of("BD"), TokenSigner.Options.defaults()), profile))
        .isTrue();
  }
}
