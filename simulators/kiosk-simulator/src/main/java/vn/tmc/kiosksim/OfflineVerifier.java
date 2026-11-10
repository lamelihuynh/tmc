package vn.tmc.kiosksim;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * What the kiosk can check alone when the edge is unreachable (ADR-11 P6, ADR-12 R2): structure and
 * the PII guard, cached device key and revocation, signature, time rules, and the local replay
 * cache. It uses the SAME rules as the server so an offline decision rarely disagrees with the
 * online one.
 *
 * <p>Fail-secure (DP-04): no cached key, a revoked key or any doubt means DENY, never open.
 */
public class OfflineVerifier {
  private static final Set<String> ALLOWED = Set.of("sub", "jti", "iat", "exp", "fac", "nonce");

  public record Verdict(boolean valid, String reason, String employeeUuid, String jti) {
    static Verdict denied(String reason) {
      return new Verdict(false, reason, null, null);
    }
  }

  private final Journal journal;
  private final Duration clockSkew;
  private final Duration maxLifetime;
  private final Duration replayWindow;

  public OfflineVerifier(
      Journal journal, Duration clockSkew, Duration maxLifetime, Duration replayWindow) {
    this.journal = journal;
    this.clockSkew = clockSkew;
    this.maxLifetime = maxLifetime;
    this.replayWindow = replayWindow;
  }

  public Verdict verify(String token, Instant now) {
    try {
      var jwt = SignedJWT.parse(token);
      if (!JWSAlgorithm.ES256.equals(jwt.getHeader().getAlgorithm())
          || jwt.getHeader().getKeyID() == null) {
        return Verdict.denied("MALFORMED_TOKEN");
      }
      var set = jwt.getJWTClaimsSet();
      var extra = new HashSet<>(set.getClaims().keySet());
      extra.removeAll(ALLOWED);
      if (!extra.isEmpty()
          || set.getSubject() == null
          || set.getJWTID() == null
          || set.getIssueTime() == null
          || set.getExpirationTime() == null) {
        return Verdict.denied("MALFORMED_TOKEN");
      }
      var key = journal.deviceKey(jwt.getHeader().getKeyID()).orElse(null);
      if (key == null) {
        return Verdict.denied("DEVICE_UNKNOWN"); // nothing synced yet: do not open (ADR-11 2.3)
      }
      if (key.revoked()) {
        return Verdict.denied("DEVICE_REVOKED");
      }
      if (!key.employeeUuid().equals(UUID.fromString(set.getSubject()).toString())) {
        return Verdict.denied("INVALID_SIGNATURE");
      }
      var pub =
          (ECPublicKey)
              KeyFactory.getInstance("EC")
                  .generatePublic(
                      new X509EncodedKeySpec(
                          java.util.Base64.getDecoder().decode(key.publicKeyBase64())));
      if (!jwt.verify(new ECDSAVerifier(pub))) {
        return Verdict.denied("INVALID_SIGNATURE");
      }
      var iat = set.getIssueTime().toInstant();
      var exp = set.getExpirationTime().toInstant();
      if (!exp.isAfter(iat) || Duration.between(iat, exp).compareTo(maxLifetime) > 0) {
        return Verdict.denied("MALFORMED_TOKEN");
      }
      if (iat.isAfter(now.plus(clockSkew))) {
        return Verdict.denied("CLOCK_SKEW_EXCEEDED");
      }
      if (now.isAfter(exp.plus(clockSkew))) {
        return Verdict.denied("QR_EXPIRED");
      }
      if (journal.jtiSeenSince(set.getJWTID(), now.minus(replayWindow))) {
        return Verdict.denied("QR_ALREADY_USED"); // ADR-12 R2
      }
      return new Verdict(true, null, key.employeeUuid(), set.getJWTID());
    } catch (Exception e) {
      return Verdict.denied("MALFORMED_TOKEN");
    }
  }
}
