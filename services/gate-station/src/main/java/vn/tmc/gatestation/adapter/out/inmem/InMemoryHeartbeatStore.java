package vn.tmc.gatestation.adapter.out.inmem;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.gatestation.application.port.in.IKioskSupport.Heartbeat;
import vn.tmc.gatestation.application.port.out.IHeartbeatStore;

/** Keeps only the latest heartbeat per kiosk, in process memory. */
public class InMemoryHeartbeatStore implements IHeartbeatStore {
  private record Seen(Heartbeat heartbeat, Instant at) {}

  private final Map<String, Seen> latest = new ConcurrentHashMap<>();

  @Override
  public void record(Heartbeat heartbeat, Instant receivedAt) {
    latest.put(heartbeat.kioskId(), new Seen(heartbeat, receivedAt));
  }

  @Override
  public Optional<Instant> lastSeen(String kioskId) {
    return Optional.ofNullable(latest.get(kioskId)).map(Seen::at);
  }
}
