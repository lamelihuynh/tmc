package vn.tmc.gatestation.adapter.out.inmem;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import vn.tmc.gatestation.application.port.out.ITokenReservation;
import vn.tmc.platform.kernel.IClock;

/**
 * One-time-use state in process memory, with the same behaviour as the Redis adapter (SET NX with
 * expiry). For tests and the "local" profile only: the state is lost on restart and is not shared
 * between nodes.
 */
public class InMemoryTokenReservation implements ITokenReservation {
  private record Hold(UUID attemptId, boolean used, Instant until) {}

  private final Map<String, Hold> holds = new HashMap<>();
  private final IClock clock;

  public InMemoryTokenReservation(IClock clock) {
    this.clock = clock;
  }

  @Override
  public synchronized Reservation reserve(String jti, UUID attemptId, Duration ttl) {
    var now = clock.now();
    var existing = holds.get(jti);
    if (existing != null && existing.until().isAfter(now)) {
      return existing.attemptId().equals(attemptId)
          ? Reservation.SAME_ATTEMPT
          : Reservation.OTHER_ATTEMPT;
    }
    holds.put(jti, new Hold(attemptId, false, now.plus(ttl)));
    return Reservation.ACQUIRED;
  }

  @Override
  public synchronized boolean ownedBy(String jti, UUID attemptId) {
    var hold = holds.get(jti);
    return hold != null && hold.until().isAfter(clock.now()) && hold.attemptId().equals(attemptId);
  }

  @Override
  public synchronized void release(String jti, UUID attemptId) {
    var existing = holds.get(jti);
    if (existing != null && existing.attemptId().equals(attemptId) && !existing.used()) {
      holds.remove(jti);
    }
  }

  @Override
  public synchronized void markUsed(String jti, UUID attemptId, Instant blockUntil) {
    var existing = holds.get(jti);
    if (existing != null
        && existing.until().isAfter(clock.now())
        && !existing.attemptId().equals(attemptId)) {
      throw new IllegalStateException("token is owned by another attempt");
    }
    holds.put(jti, new Hold(attemptId, true, blockUntil));
  }
}
