package vn.tmc.userqr.adapter.out.inmem;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

  @Override
  public void save(Device device) {
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
