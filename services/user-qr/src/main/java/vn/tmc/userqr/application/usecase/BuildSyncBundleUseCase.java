package vn.tmc.userqr.application.usecase;

import java.time.Instant;
import java.util.Base64;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.userqr.application.port.in.ISyncBundle;
import vn.tmc.userqr.application.port.out.IDeviceKeyRegistry;

public class BuildSyncBundleUseCase implements ISyncBundle {
  private final IDeviceKeyRegistry registry;
  private final IClock clock;

  public BuildSyncBundleUseCase(IDeviceKeyRegistry registry, IClock clock) {
    this.registry = registry;
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
    return new Bundle(clock.now(), keys);
  }
}
