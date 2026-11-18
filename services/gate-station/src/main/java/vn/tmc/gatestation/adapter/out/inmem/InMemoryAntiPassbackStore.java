package vn.tmc.gatestation.adapter.out.inmem;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.gatestation.application.port.out.IAntiPassbackStore;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.domain.LastPass;
import vn.tmc.platform.kernel.IClock;

/**
 * Anti-passback state in process memory, with the same behaviour as the Redis adapter. For tests
 * and the "local" profile only.
 */
public class InMemoryAntiPassbackStore implements IAntiPassbackStore {
  private record Pending(LastPass pass, Instant expiresAt) {}

  private final Map<UUID, LastPass> last = new HashMap<>();
  private final Map<UUID, Pending> pending = new HashMap<>();
  private final IClock clock;

  public InMemoryAntiPassbackStore(IClock clock) {
    this.clock = clock;
  }

  @Override
  public synchronized Optional<LastPass> last(UUID employeeUuid) {
    return Optional.ofNullable(last.get(employeeUuid));
  }

  @Override
  public synchronized boolean tryReserve(
      UUID employeeUuid, Direction direction, Instant scannedAt, Duration pendingTtl) {
    var now = clock.now();
    var existing = pending.get(employeeUuid);
    if (existing != null && existing.expiresAt().isAfter(now)) {
      return false;
    }
    pending.put(
        employeeUuid, new Pending(new LastPass(direction, scannedAt), now.plus(pendingTtl)));
    return true;
  }

  @Override
  public synchronized void confirm(UUID employeeUuid) {
    var slot = pending.remove(employeeUuid);
    if (slot != null) {
      last.put(employeeUuid, slot.pass());
    }
  }

  @Override
  public synchronized void release(UUID employeeUuid) {
    pending.remove(employeeUuid);
  }

  @Override
  public synchronized void keepPending(UUID employeeUuid, Duration ttl) {
    var slot = pending.get(employeeUuid);
    if (slot != null) {
      pending.put(employeeUuid, new Pending(slot.pass(), clock.now().plus(ttl)));
    }
  }
}
