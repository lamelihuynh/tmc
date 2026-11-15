package vn.tmc.platform.security.jwt;

import java.time.Duration;

/**
 * Fixed values of the access token contract (docs/api/identity.openapi.yaml). The issuer (identity)
 * and every verifier read them from here so they cannot drift apart.
 */
public final class AccessTokenContract {
  private AccessTokenContract() {}

  public static final String ISSUER = "tmc-identity";
  public static final String AUDIENCE = "tmc-services";

  /** NFR-03: 15 minutes; there is no refresh token yet. */
  public static final Duration TTL = Duration.ofMinutes(15);

  public static final Duration CLOCK_SKEW = Duration.ofSeconds(30);

  public static final String CLAIM_ROLES = "roles";
  public static final String CLAIM_FACTORIES = "fac";
  public static final String CLAIM_GATES = "gates";

  /** {@code fac = ["*"]} means every factory ({@code DataScope.all()}). */
  public static final String ALL_FACTORIES = "*";

  public static final String JWKS_PATH = "/.well-known/jwks.json";
}
