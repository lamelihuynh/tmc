package vn.tmc.gatestation.application.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.domain.LastPass;

/**
 * Driven port: anti-passback state per employee (FR-17). Two things are kept: the last CONFIRMED
 * pass, which the rule reads, and at most one PENDING slot, which stops two scans of the same
 * person from running at the same time.
 */
public interface IAntiPassbackStore {
  /** The last confirmed pass, empty when the employee has none on record. */
  Optional<LastPass> last(UUID employeeUuid);

  /**
   * Atomic: takes the pending slot for this employee.
   *
   * @param pendingTtl the slot frees itself after this time if nobody confirms or releases it
   * @return false when another scan of the same employee is still pending
   */
  boolean tryReserve(
      UUID employeeUuid, Direction direction, Instant scannedAt, Duration pendingTtl);

  /** The gate opened: the pending pass becomes the last confirmed pass. */
  void confirm(UUID employeeUuid);

  /** The gate did not open: drop the pending slot, the last confirmed pass is unchanged. */
  void release(UUID employeeUuid);

  /** Outcome unknown: keep the slot taken for {@code ttl} from now, until reconcile decides. */
  void keepPending(UUID employeeUuid, Duration ttl);
}
