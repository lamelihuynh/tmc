package vn.tmc.attendance.application.usecase;

import java.time.Duration;

/**
 * Timing knobs for the recovery job (configuration over code, DP-10).
 *
 * @param reservedTtl RESERVED rows older than this are released as DENIED/SYSTEM_RETRY (P2)
 * @param commandGrace GATE_COMMANDED rows older than this become GATE_UNKNOWN (P3, P4)
 */
public record RecoveryPolicy(Duration reservedTtl, Duration commandGrace, int batchSize) {
  public static RecoveryPolicy defaults() {
    return new RecoveryPolicy(Duration.ofSeconds(30), Duration.ofSeconds(10), 100);
  }
}
