package vn.tmc.gatestation.support;

import java.time.Duration;
import java.time.Instant;
import vn.tmc.platform.kernel.IClock;

public class MutableClock implements IClock {
  private Instant now = Instant.parse("2026-11-09T06:45:00Z");

  @Override
  public Instant now() {
    return now;
  }

  public void advance(Duration d) {
    now = now.plus(d);
  }
}
