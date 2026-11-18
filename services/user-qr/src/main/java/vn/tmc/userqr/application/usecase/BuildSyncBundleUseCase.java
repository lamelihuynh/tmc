package vn.tmc.userqr.application.usecase;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.userqr.application.port.in.ISyncBundle;
import vn.tmc.userqr.application.port.out.IDeviceKeyRegistry;
import vn.tmc.userqr.application.port.out.IEmployeeDirectory;

public class BuildSyncBundleUseCase implements ISyncBundle {
  private final IDeviceKeyRegistry registry;
  private final IEmployeeDirectory employees;
  private final IClock clock;

  public BuildSyncBundleUseCase(IDeviceKeyRegistry registry, IClock clock) {
    this(registry, since -> List.of(), clock);
  }

  public BuildSyncBundleUseCase(
      IDeviceKeyRegistry registry, IEmployeeDirectory employees, IClock clock) {
    this.registry = registry;
    this.employees = employees;
    this.clock = clock;
  }

  @Override
  public Bundle changedSince(Instant since) {
    var keys =
        registry.changedSince(since).stream()
            .map(
                d ->
                    new DeviceKey(
                        d.deviceId(),
                        d.employeeUuid(),
                        Base64.getEncoder().encodeToString(d.publicKey().getEncoded()),
                        !d.isActive()))
            .toList();
    var eligibility =
        employees.changedSince(since).stream()
            .map(
                e ->
                    new Employee(
                        e.employeeUuid(),
                        e.active() ? "ACTIVE" : "INACTIVE",
                        e.factories().stream().sorted().toList(),
                        e.updatedAt()))
            .toList();
    return new Bundle(clock.now(), keys, eligibility);
  }
}
