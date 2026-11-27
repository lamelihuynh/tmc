package vn.tmc.identity.adapter.out.crypto;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import vn.tmc.platform.security.jwt.AccessTokenSigner;

/**
 * The keys identity publishes (C-12, NFR-03): the current key signs every new token; the previous
 * key, public half only, stays in the JWKS during a rotation so tokens it signed are still accepted
 * until they expire. Verifiers pick the key by the {@code kid} of the token header
 * (JwtPrincipalVerifier), so they need no change.
 *
 * <p>Rotation: start identity with the new private key and the old public key as previous; after
 * the last old token has expired (15 minutes) plus the verifiers' JWKS cache (5 minutes), remove
 * the previous key.
 */
public final class SigningKeys {
  private final AccessTokenSigner current;
  private final ECKey previous;

  public SigningKeys(AccessTokenSigner current, ECKey previous) {
    this.current = current;
    this.previous = previous == null ? null : publicSigningKey(previous, current.keyId());
  }

  /**
   * @param previousJwk JSON of the previous EC P-256 key; blank means no rotation in progress. A
   *     private JWK is accepted but only its public half is kept and published.
   */
  public static SigningKeys of(AccessTokenSigner current, String previousJwk) {
    if (previousJwk == null || previousJwk.isBlank()) {
      return new SigningKeys(current, null);
    }
    try {
      return new SigningKeys(current, ECKey.parse(previousJwk));
    } catch (ParseException e) {
      throw new IllegalArgumentException("previous signing key is not an EC JWK", e);
    }
  }

  /** Signs new tokens. */
  public AccessTokenSigner current() {
    return current;
  }

  public Optional<String> previousKeyId() {
    return Optional.ofNullable(previous).map(ECKey::getKeyID);
  }

  /** Body of {@code GET /.well-known/jwks.json}: current key first, then the previous one. */
  public String publicJwksJson() {
    List<com.nimbusds.jose.jwk.JWK> keys = new ArrayList<>();
    keys.add(current.publicJwk());
    if (previous != null) {
      keys.add(previous);
    }
    return new JWKSet(keys).toString(true);
  }

  private static ECKey publicSigningKey(ECKey key, String currentKid) {
    if (!Curve.P_256.equals(key.getCurve())) {
      throw new IllegalArgumentException("previous signing key must be EC P-256 (ES256)");
    }
    if (key.getKeyID() == null || key.getKeyID().isBlank()) {
      throw new IllegalArgumentException("previous signing key needs a kid");
    }
    if (key.getKeyID().equals(currentKid)) {
      throw new IllegalArgumentException(
          "previous signing key has the same kid as the current one: " + currentKid);
    }
    return new ECKey.Builder(key.toPublicJWK())
        .keyUse(KeyUse.SIGNATURE)
        .algorithm(JWSAlgorithm.ES256)
        .build();
  }
}
