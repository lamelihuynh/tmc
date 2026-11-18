package vn.tmc.platform.security.jwt;

import com.nimbusds.jose.util.Base64URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;

/**
 * Access tokens for tests, signed with {@link TestJwtKeys}. Valid ones have exactly the claim set
 * identity issues. Send them as {@code Authorization: Bearer <token>} ({@link #bearer}).
 */
public final class TestTokens {
  private TestTokens() {}

  public static Principal principal(UUID subject, DataScope scope, Role... roles) {
    return new Principal(subject, Set.of(roles), scope);
  }

  /** A valid token issued now. */
  public static String valid(Principal p) {
    return validAt(p, Instant.now());
  }

  public static String valid(UUID subject, Role... roles) {
    return valid(principal(subject, new DataScope(Set.of(), Set.of(), Set.of(), false), roles));
  }

  public static String validAt(Principal p, Instant issuedAt) {
    return TestJwtKeys.TRUSTED.sign(p, issuedAt, jti());
  }

  /** Issued 16 minutes ago with the normal 15 minute life: expired (beyond the 30 s skew). */
  public static String expired(Principal p) {
    return TestJwtKeys.TRUSTED.sign(p, Instant.now().minus(Duration.ofMinutes(16)), jti());
  }

  /** Correct claims, signed by a key the verifier does not trust. */
  public static String signedByUntrustedKey(Principal p) {
    return TestJwtKeys.UNTRUSTED.sign(p, Instant.now(), jti());
  }

  public static String withIssuer(Principal p, String issuer) {
    return TestJwtKeys.TRUSTED.sign(
        AccessTokenSigner.claims(p, Instant.now(), AccessTokenContract.TTL, jti())
            .issuer(issuer)
            .build());
  }

  public static String withAudience(Principal p, String audience) {
    return TestJwtKeys.TRUSTED.sign(
        AccessTokenSigner.claims(p, Instant.now(), AccessTokenContract.TTL, jti())
            .audience(audience)
            .build());
  }

  /** A role name outside the enum: the verifier must refuse the whole token. */
  public static String withRoles(Principal p, List<String> roles) {
    return TestJwtKeys.TRUSTED.sign(
        AccessTokenSigner.claims(p, Instant.now(), AccessTokenContract.TTL, jti())
            .claim(AccessTokenContract.CLAIM_ROLES, roles)
            .build());
  }

  /** The payload of a valid token with header {@code "alg":"none"} and no signature. */
  public static String unsigned(Principal p) {
    var signed = valid(p);
    var payload = signed.split("\\.")[1];
    var header = Base64URL.encode("{\"alg\":\"none\",\"typ\":\"JWT\"}").toString();
    return header + "." + payload + ".";
  }

  public static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String jti() {
    return UUID.randomUUID().toString();
  }
}
