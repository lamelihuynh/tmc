package vn.tmc.platform.kernel;

import java.time.Instant;

/**
 * Time source port. Business code never calls {@code Instant.now()} directly (testability, ADR-12
 * clock skew).
 */
public interface IClock {
  Instant now();

  static IClock system() {
    return Instant::now;
  }
}
