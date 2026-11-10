package vn.tmc.userqr.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Time rules for a token whose signature and device were already checked (ADR-12 sections 3 and
 * tests 6).
 *
 * @param clockSkew tolerated difference between phone and server clocks (default 60 s)
 * @param maxLifetime longest exp-iat accepted; rejects "static-like" long lived tokens (default 60
 *     s)
 */
public record TokenVerificationPolicy(Duration clockSkew, Duration maxLifetime) {

  public static TokenVerificationPolicy defaults() {
    return new TokenVerificationPolicy(Duration.ofSeconds(60), Duration.ofSeconds(60));
  }

  /**
   * @return the denial reason, or empty when the token is acceptable at {@code now}.
   */
  public Optional<ReasonCode> check(QrClaims claims, Instant now) {
    if (Duration.between(claims.issuedAt(), claims.expiresAt()).compareTo(maxLifetime) > 0
        || !claims.expiresAt().isAfter(claims.issuedAt())) {
      return Optional.of(ReasonCode.MALFORMED_TOKEN);
    }
    if (claims.issuedAt().isAfter(now.plus(clockSkew))) {
      return Optional.of(ReasonCode.CLOCK_SKEW_EXCEEDED);
    }
    if (now.isAfter(claims.expiresAt().plus(clockSkew))) {
      return Optional.of(ReasonCode.QR_EXPIRED);
    }
    return Optional.empty();
  }
}
