package vn.tmc.gatestation.application.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Driven port: one-time use of a QR token id (ADR-12 replay layer R1, ADR-13 step 3). The token is
 * first held for a short time while the gate command runs, then either marked used (gate opened or
 * outcome unknown) or released (gate definitely did not open, so the person can rescan).
 */
public interface ITokenReservation {
  enum Reservation {
    ACQUIRED,
    SAME_ATTEMPT,
    OTHER_ATTEMPT
  }

  /**
   * Atomic: only one attempt owns a jti; a retry by that attempt is distinguishable from reuse.
   *
   * @param ttl the hold expires by itself after this time, so a crash cannot strand a token
   * @return whether this attempt acquired the token, already owns it, or another attempt owns it
   */
  Reservation reserve(String jti, UUID attemptId, Duration ttl);

  /** Whether this attempt already owns a live reservation or used-token marker. */
  boolean ownedBy(String jti, UUID attemptId);

  /** Gives a held token back. A token already marked used stays used. */
  void release(String jti, UUID attemptId);

  /** Blocks the token id until {@code blockUntil} (its expiry plus the clock skew). */
  void markUsed(String jti, UUID attemptId, Instant blockUntil);
}
