package vn.tmc.userqr.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.userqr.domain.Device;

/** Driven port: the Device Key Registry (ADR-12). Public keys only. */
public interface IDeviceKeyRegistry {
  Optional<Device> findById(String deviceId);

  Optional<Device> findActiveByEmployee(UUID employeeUuid);

  /**
   * Insert or replace by device id.
   *
   * @throws ActiveDeviceConflictException when {@code device} is ACTIVE and the employee already
   *     has another ACTIVE device (at most one per employee, ADR-12)
   */
  void save(Device device);

  /** Devices registered or revoked strictly after {@code since}, oldest change first. */
  List<Device> changedSince(Instant since);
}
