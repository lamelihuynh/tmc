package vn.tmc.platform.security.jwt;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;

/**
 * Checks an access token issued by identity and turns it into a {@link Principal}
 * (docs/api/identity.openapi.yaml, "Kiểm tra token ở service"). Any failure gives {@code null},
 * never a partial principal: the caller treats it as unauthenticated. Services still run {@code
 * Rbac.require*} themselves (ADR-08).
 */
public final class JwtPrincipalVerifier {
  private final DefaultJWTProcessor<SecurityContext> processor;
  private final IClock clock;

  public JwtPrincipalVerifier(JWKSource<SecurityContext> keys, IClock clock) {
    this.clock = clock;
    this.processor = new DefaultJWTProcessor<>();
    processor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(JOSEObjectType.JWT));
    // Only ES256: an "alg":"none" or HS256 token has no matching key selector and is refused.
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, keys));
    // Claims are checked below against the injected clock, so tests are deterministic.
    processor.setJWTClaimsSetVerifier(null);
  }

  /**
   * Keys fetched from identity's JWKS endpoint, cached (5 minutes) and refetched on unknown kid.
   */
  public static JwtPrincipalVerifier fromJwksUrl(String jwksUrl, IClock clock) {
    try {
      JWKSource<SecurityContext> source =
          JWKSourceBuilder.create(URI.create(jwksUrl).toURL()).retrying(true).build();
      return new JwtPrincipalVerifier(source, clock);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("bad JWKS url " + jwksUrl, e);
    }
  }

  /** Fixed keys, for tests and for a service started with a pinned JWKS document. */
  public static JwtPrincipalVerifier fromJwksJson(String jwksJson, IClock clock) {
    try {
      return new JwtPrincipalVerifier(new ImmutableJWKSet<>(JWKSet.parse(jwksJson)), clock);
    } catch (ParseException e) {
      throw new IllegalArgumentException("not a JWKS document", e);
    }
  }

  /**
   * @return the principal, or null when the token is missing, malformed, badly signed, expired, for
   *     another issuer or audience, or carries an unknown role
   */
  public Principal verify(String token) {
    if (token == null || token.isBlank()) {
      return null;
    }
    try {
      JWTClaimsSet claims = processor.process(token, null);
      return toPrincipal(claims);
    } catch (Exception e) {
      return null; // bad signature, unknown kid, parse error, wrong type...: all unauthenticated
    }
  }

  private Principal toPrincipal(JWTClaimsSet c) throws ParseException {
    var now = clock.now();
    var skew = AccessTokenContract.CLOCK_SKEW;
    if (!AccessTokenContract.ISSUER.equals(c.getIssuer())) {
      return null;
    }
    if (c.getAudience() == null || !c.getAudience().contains(AccessTokenContract.AUDIENCE)) {
      return null;
    }
    Date exp = c.getExpirationTime();
    Date iat = c.getIssueTime();
    if (exp == null || iat == null || c.getJWTID() == null || c.getSubject() == null) {
      return null;
    }
    if (!now.isBefore(exp.toInstant().plus(skew))) {
      return null;
    }
    if (iat.toInstant().isAfter(now.plus(skew))) {
      return null;
    }
    UUID subject = UUID.fromString(c.getSubject());
    List<String> roleNames = c.getStringListClaim(AccessTokenContract.CLAIM_ROLES);
    if (roleNames == null) {
      return null;
    }
    Set<Role> roles = new HashSet<>();
    for (String r : roleNames) {
      roles.add(Role.valueOf(r)); // an unknown role throws: the whole token is refused
    }
    List<String> fac = c.getStringListClaim(AccessTokenContract.CLAIM_FACTORIES);
    List<String> gates = c.getStringListClaim(AccessTokenContract.CLAIM_GATES);
    DataScope scope =
        fac != null && fac.contains(AccessTokenContract.ALL_FACTORIES)
            ? DataScope.all()
            : new DataScope(
                fac == null ? Set.of() : Set.copyOf(fac),
                Set.of(),
                gates == null ? Set.of() : Set.copyOf(gates),
                false);
    return new Principal(subject, roles, scope);
  }
}
