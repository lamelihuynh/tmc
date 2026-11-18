package vn.tmc.userqr.adapter.out.crypto;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.HashSet;
import java.util.UUID;
import vn.tmc.platform.kernel.ReasonCode;
import vn.tmc.userqr.application.port.out.ITokenCodec;
import vn.tmc.userqr.domain.QrClaims;

/**
 * JWS compact, ES256 only (ADR-12). Anything else is MALFORMED_TOKEN: another algorithm (including
 * "none"), a missing kid, a claim outside {@link QrClaims#ALLOWED_CLAIM_NAMES} (PII guard, ADR-09),
 * or an oversized string.
 */
public class NimbusTokenCodec implements ITokenCodec {
  /** A real token is about 350 characters; a QR code should never carry more than this. */
  private static final int MAX_TOKEN_LENGTH = 2048;

  @Override
  public Parsed parse(String compactToken) {
    if (compactToken == null || compactToken.isBlank()) {
      throw malformed("empty token");
    }
    if (compactToken.length() > MAX_TOKEN_LENGTH) {
      throw malformed("token too long");
    }
    try {
      var jwt = SignedJWT.parse(compactToken);
      var header = jwt.getHeader();
      if (!JWSAlgorithm.ES256.equals(header.getAlgorithm())) {
        throw malformed("algorithm must be ES256");
      }
      var kid = header.getKeyID();
      if (kid == null || kid.isBlank()) {
        throw malformed("kid is missing");
      }
      return new Parsed(kid, toClaims(jwt.getJWTClaimsSet()));
    } catch (TokenFormatException e) {
      throw e;
    } catch (Exception e) {
      throw malformed("not a readable JWS");
    }
  }

  private static QrClaims toClaims(JWTClaimsSet set) throws ParseException {
    for (var name : set.getClaims().keySet()) {
      if (!QrClaims.ALLOWED_CLAIM_NAMES.contains(name)) {
        throw malformed("claim not allowed: " + name);
      }
    }
    if (set.getSubject() == null
        || set.getJWTID() == null
        || set.getJWTID().isBlank()
        || set.getIssueTime() == null
        || set.getExpirationTime() == null) {
      throw malformed("sub, jti, iat and exp are required");
    }
    UUID subject;
    try {
      subject = UUID.fromString(set.getSubject());
    } catch (IllegalArgumentException e) {
      throw malformed("sub must be a UUID");
    }
    var fac = set.getStringListClaim("fac");
    return new QrClaims(
        subject,
        set.getJWTID(),
        set.getIssueTime().toInstant(),
        set.getExpirationTime().toInstant(),
        fac == null ? null : new HashSet<>(fac),
        set.getStringClaim("nonce"));
  }

  @Override
  public boolean verifySignature(String compactToken, PublicKey key) {
    if (!(key instanceof ECPublicKey ecKey)) {
      return false;
    }
    try {
      return SignedJWT.parse(compactToken).verify(new ECDSAVerifier(ecKey));
    } catch (Exception e) {
      return false; // fail-secure (DP-04)
    }
  }

  private static TokenFormatException malformed(String message) {
    return new TokenFormatException(ReasonCode.MALFORMED_TOKEN, message);
  }
}
