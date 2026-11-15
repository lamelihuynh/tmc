package vn.tmc.platform.security.jwt;

import vn.tmc.platform.kernel.IClock;

/**
 * Two fixed EC P-256 key pairs for TESTS ONLY. They are public in the repository: never configure
 * them in a running service outside a test. {@link #TRUSTED} is the key the test verifier trusts;
 * {@link #UNTRUSTED} signs tokens that must be refused.
 *
 * <p>Use from any module: {@code testImplementation(testFixtures(project(":platform:security")))}.
 */
public final class TestJwtKeys {
  private TestJwtKeys() {}

  public static final String TRUSTED_PRIVATE_JWK =
      "{\"kty\":\"EC\",\"crv\":\"P-256\",\"kid\":\"test-key-a\","
          + "\"x\":\"tb4Q7Hun_PZdDJa5LldYfXSdANzOsEInsqLOJneP7k8\","
          + "\"y\":\"OWJQkdmnF6WxDBwp_YLDjBJ1CmBa3Lid_UOv039nk9I\","
          + "\"d\":\"3a8wiNjGdeukoOO3FN8fpRkzqkU_0AUvt_XbR13_kgQ\"}";

  public static final String UNTRUSTED_PRIVATE_JWK =
      "{\"kty\":\"EC\",\"crv\":\"P-256\",\"kid\":\"test-key-b\","
          + "\"x\":\"ubhARWnJJ3S1X4Q3p2yeGDONNvjs8707jggd_NMt624\","
          + "\"y\":\"IDeoEZEzeIDAkxhURgxwBUNRHpgdDYoiOW372P8Cs_g\","
          + "\"d\":\"8fd2qZHYsRr-2zi2n5K5V7xSfef6g-3rNfdBk3XekBM\"}";

  public static final AccessTokenSigner TRUSTED =
      AccessTokenSigner.fromPrivateJwk(TRUSTED_PRIVATE_JWK);

  public static final AccessTokenSigner UNTRUSTED =
      AccessTokenSigner.fromPrivateJwk(UNTRUSTED_PRIVATE_JWK);

  /** JWKS document with only the trusted public key, as identity would serve it. */
  public static String trustedJwksJson() {
    return TRUSTED.publicJwksJson();
  }

  /** A verifier that trusts {@link #TRUSTED}, on the given clock. */
  public static JwtPrincipalVerifier verifier(IClock clock) {
    return JwtPrincipalVerifier.fromJwksJson(trustedJwksJson(), clock);
  }

  public static JwtPrincipalVerifier verifier() {
    return verifier(IClock.system());
  }
}
