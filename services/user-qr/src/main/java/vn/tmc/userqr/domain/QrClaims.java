package vn.tmc.userqr.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Claims of a QR token. Deliberately tiny: subject is an internal UUID, no readable PII (ADR-09,
 * ADR-12). Anything outside this set is rejected when the token is parsed.
 */
public record QrClaims(
    UUID subject,
    String jti,
    Instant issuedAt,
    Instant expiresAt,
    Set<String> factories,
    String nonce) {
  public static final Set<String> ALLOWED_CLAIM_NAMES =
      Set.of("sub", "jti", "iat", "exp", "fac", "nonce");

  public QrClaims {
    factories = factories == null ? Set.of() : Set.copyOf(factories);
  }
}
