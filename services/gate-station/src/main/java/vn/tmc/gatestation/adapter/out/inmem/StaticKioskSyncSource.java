package vn.tmc.gatestation.adapter.out.inmem;

import java.time.Instant;
import java.util.List;
import vn.tmc.gatestation.application.port.in.IKioskSupport.DeviceKey;
import vn.tmc.gatestation.application.port.out.IKioskSyncSource;

/** No device keys: for the inmem profile, where there is no User/QR service to ask. */
public class StaticKioskSyncSource implements IKioskSyncSource {
  @Override
  public List<DeviceKey> deviceKeysChangedSince(Instant since) {
    return List.of();
  }
}
