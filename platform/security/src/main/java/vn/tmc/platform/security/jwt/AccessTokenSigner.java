package vn.tmc.platform.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;

/**
 * Signs access tokens with exactly the claim set of the contract (ES256, typ JWT, kid). Identity
 * uses it to issue tokens; the test fixtures use it so test tokens are byte-for-byte the shape
 * identity produces. Only identity holds a production private key.
 */
public final class AccessTokenSigner {
  private final ECKey key;
  private final ECDSASigner signer;

  public AccessTokenSigner(ECKey privateKey) {
    if (!privateKey.isPrivate() || privateKey.getKeyID() == null) {
      throw new IllegalArgumentException("need a private EC key with a kid");
    }
    if (!Curve.P_256.equals(privateKey.getCurve())) {
      throw new IllegalArgumentException("ES256 needs a P-256 key");
    }
    this.key = privateKey;
    try {
      this.signer = new ECDSASigner(privateKey);
    } catch (JOSEException e) {
      throw new IllegalArgumentException("unusable EC key", e);
    }
  }

  /** Parses a private JWK in JSON (for keys loaded from configuration). */
  public static AccessTokenSigner fromPrivateJwk(String json) {
    try {
      return new AccessTokenSigner(ECKey.parse(json));
    } catch (ParseException e) {
      throw new IllegalArgumentException("not an EC JWK", e);
    }
  }

  /**
   * A fresh key that lives as long as the process (dev default). Its kid is the key thumbprint, so
   * a restart gives a new kid and verifiers refetch the JWKS instead of using a stale key.
   */
  public static AccessTokenSigner ephemeral() {
    try {
      var generated = new ECKeyGenerator(Curve.P_256).generate();
      var kid = "identity-" + generated.computeThumbprint().toString().substring(0, 16);
      return new AccessTokenSigner(new ECKey.Builder(generated).keyID(kid).build());
    } catch (JOSEException e) {
      throw new IllegalStateException("cannot generate an EC key", e);
    }
  }

  public String keyId() {
    return key.getKeyID();
  }

  /** The public half as a JWKS document, the body of {@code GET /.well-known/jwks.json}. */
  public String publicJwksJson() {
    return new JWKSet(publicJwk()).toString(true);
  }

  public ECKey publicJwk() {
    return new ECKey.Builder(key.toPublicJWK())
        .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
        .algorithm(JWSAlgorithm.ES256)
        .build();
  }

  /** Signs the contract claim set for {@code principal}, valid {@link AccessTokenContract#TTL}. */
  public String sign(Principal principal, Instant issuedAt, String jti) {
    return sign(principal, issuedAt, AccessTokenContract.TTL, jti);
  }

  /** As {@link #sign(Principal, Instant, String)} with another lifetime: tests only. */
  public String sign(Principal principal, Instant issuedAt, Duration ttl, String jti) {
    return sign(claims(principal, issuedAt, ttl, jti).build());
  }

  /** Signs any claim set: for tests that need a token which breaks the contract. */
  public String sign(JWTClaimsSet claims) {
    var header =
        new JWSHeader.Builder(JWSAlgorithm.ES256)
            .keyID(key.getKeyID())
            .type(JOSEObjectType.JWT)
            .build();
    var jwt = new SignedJWT(header, claims);
    try {
      jwt.sign(signer);
    } catch (JOSEException e) {
      throw new IllegalStateException("signing failed", e);
    }
    return jwt.serialize();
  }

  /** The contract claim set, as a builder so tests can tamper with one claim. */
  public static JWTClaimsSet.Builder claims(
      Principal principal, Instant issuedAt, Duration ttl, String jti) {
    var builder =
        new JWTClaimsSet.Builder()
            .issuer(AccessTokenContract.ISSUER)
            .audience(AccessTokenContract.AUDIENCE)
            .subject(principal.subject().toString())
            .claim(
                AccessTokenContract.CLAIM_ROLES,
                principal.roles().stream().map(Enum::name).sorted().toList())
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(issuedAt.plus(ttl)))
            .jwtID(jti);
    DataScope scope = principal.scope();
    if (scope != null) {
      if (scope.allFactories()) {
        builder.claim(
            AccessTokenContract.CLAIM_FACTORIES, List.of(AccessTokenContract.ALL_FACTORIES));
      } else if (!scope.factories().isEmpty()) {
        builder.claim(
            AccessTokenContract.CLAIM_FACTORIES, scope.factories().stream().sorted().toList());
      }
      if (!scope.gates().isEmpty()) {
        builder.claim(AccessTokenContract.CLAIM_GATES, scope.gates().stream().sorted().toList());
      }
    }
    return builder;
  }
}
