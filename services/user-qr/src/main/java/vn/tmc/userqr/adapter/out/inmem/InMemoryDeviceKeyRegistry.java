package vn.tmc.userqr.adapter.out.inmem;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.userqr.application.port.out.ActiveDeviceConflictException;
import vn.tmc.userqr.application.port.out.IDeviceKeyRegistry;
import vn.tmc.userqr.domain.Device;

/** Process-memory registry for tests and the local profile (lost on restart). */
public class InMemoryDeviceKeyRegistry implements IDeviceKeyRegistry {
  private final Map<String, Device> devices = new ConcurrentHashMap<>();

  @Override
  public Optional<Device> findById(String deviceId) {
    return deviceId == null ? Optional.empty() : Optional.ofNullable(devices.get(deviceId));
  }

  @Override
  public Optional<Device> findActiveByEmployee(UUID employeeUuid) {
    return devices.values().stream()
        .filter(d -> d.isActive() && d.employeeUuid().equals(employeeUuid))
        .findFirst();
  }

  /** Same rule as the PostgreSQL partial unique index, so both adapters keep one contract. */
  @Override
  public synchronized void save(Device device) {
    if (device.isActive()) {
      var other =
          devices.values().stream()
              .anyMatch(
                  d ->
                      d.isActive()
                          && d.employeeUuid().equals(device.employeeUuid())
                          && !d.deviceId().equals(device.deviceId()));
      if (other) {
        throw new ActiveDeviceConflictException("another ACTIVE device exists for this employee");
      }
    }
    devices.put(device.deviceId(), device);
  }

  @Override
  public List<Device> changedSince(Instant since) {
    return devices.values().stream()
        .filter(d -> d.updatedAt().isAfter(since))
        .sorted(Comparator.comparing(Device::updatedAt))
        .toList();
  }
}
