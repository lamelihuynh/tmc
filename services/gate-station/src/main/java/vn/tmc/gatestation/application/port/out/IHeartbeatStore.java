package vn.tmc.gatestation.application.port.out;

import java.time.Instant;
import java.util.Optional;
import vn.tmc.gatestation.application.port.in.IKioskSupport.Heartbeat;

/**
 * Driven port: the latest heartbeat of each kiosk (document.md 16.2 row 4). TODO Team A (A-07):
 * alert the guard when a kiosk has been silent for more than 60 seconds.
 */
public interface IHeartbeatStore {
  void record(Heartbeat heartbeat, Instant receivedAt);

  Optional<Instant> lastSeen(String kioskId);
}
