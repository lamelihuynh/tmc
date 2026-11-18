package vn.tmc.gatestation.application.port.out;

import java.time.Duration;
import java.time.Instant;

/**
 * Driven port: one-time use of a QR token id (ADR-12 replay layer R1, ADR-13 step 3). The token is
 * first held for a short time while the gate command runs, then either marked used (gate opened or
 * outcome unknown) or released (gate definitely did not open, so the person can rescan).
 */
public interface ITokenReservation {
  /**
   * Atomic: of any number of concurrent callers with the same jti, exactly one gets true.
   *
   * @param ttl the hold expires by itself after this time, so a crash cannot strand a token
   * @return false when the token id is already held or used
   */
  boolean reserve(String jti, Duration ttl);

  /** Gives a held token back. A token already marked used stays used. */
  void release(String jti);

  /** Blocks the token id until {@code blockUntil} (its expiry plus the clock skew). */
  void markUsed(String jti, Instant blockUntil);
}
