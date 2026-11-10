package vn.tmc.gatestation.application.usecase;

import java.time.Duration;

/**
 * @param reservationTtl how long a token id is held while the gate command runs (ADR-11 P2: short,
 *     so a crash cannot strand a token)
 * @param pendingTtl how long an anti-passback slot stays pending after an unknown gate outcome
 * @param tokenBlockAfterUse extra time a used token id stays blocked past its own expiry (clock
 *     skew)
 */
public record ScanSettings(
    Duration reservationTtl, Duration pendingTtl, Duration tokenBlockAfterUse) {
  public static ScanSettings defaults() {
    return new ScanSettings(Duration.ofSeconds(10), Duration.ofSeconds(60), Duration.ofSeconds(60));
  }
}
