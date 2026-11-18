package vn.tmc.gatestation.adapter.out.inmem;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import vn.tmc.gatestation.application.port.out.ITokenReservation;
import vn.tmc.platform.kernel.IClock;

/**
 * One-time-use state in process memory, with the same behaviour as the Redis adapter (SET NX with
 * expiry). For tests and the "local" profile only: the state is lost on restart and is not shared
 * between nodes.
 */
public class InMemoryTokenReservation implements ITokenReservation {
  private record Hold(boolean used, Instant until) {}

  private final Map<String, Hold> holds = new HashMap<>();
  private final IClock clock;

  public InMemoryTokenReservation(IClock clock) {
    this.clock = clock;
  }

  @Override
  public synchronized boolean reserve(String jti, Duration ttl) {
    var now = clock.now();
    var existing = holds.get(jti);
    if (existing != null && existing.until().isAfter(now)) {
      return false;
    }
    holds.put(jti, new Hold(false, now.plus(ttl)));
    return true;
  }

  @Override
  public synchronized void release(String jti) {
    var existing = holds.get(jti);
    if (existing != null && !existing.used()) {
      holds.remove(jti);
    }
  }

  @Override
  public synchronized void markUsed(String jti, Instant blockUntil) {
    holds.put(jti, new Hold(true, blockUntil));
  }
}
