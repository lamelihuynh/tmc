package vn.tmc.gatestation.application.usecase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import vn.tmc.gatestation.application.port.in.IKioskSupport;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.OfflineItem;
import vn.tmc.gatestation.application.port.out.IGateConfigRepository;
import vn.tmc.gatestation.application.port.out.IHeartbeatStore;
import vn.tmc.gatestation.application.port.out.IKioskSyncSource;
import vn.tmc.platform.kernel.IClock;

public class KioskSupportService implements IKioskSupport {
  private final IGateConfigRepository gates;
  private final IKioskSyncSource source;
  private final IHeartbeatStore heartbeats;
  private final IAttendanceGateway attendance;
  private final IClock clock;

  public KioskSupportService(
      IGateConfigRepository gates,
      IKioskSyncSource source,
      IHeartbeatStore heartbeats,
      IAttendanceGateway attendance,
      IClock clock) {
    this.attendance = attendance;
    this.gates = gates;
    this.source = source;
    this.heartbeats = heartbeats;
    this.clock = clock;
  }

  @Override
  public SyncBundle syncBundle(Instant since) {
    return new SyncBundle(clock.now(), gates.all(), source.deviceKeysChangedSince(since));
  }

  @Override
  public void heartbeat(Heartbeat heartbeat) {
    heartbeats.record(heartbeat, clock.now());
  }

  @Override
  public List<OfflineEntryResult> ingestOffline(String kioskId, List<OfflineEntry> entries) {
    var results = new ArrayList<OfflineEntryResult>();
    var forwarded = new ArrayList<OfflineItem>();
    for (var e : entries) {
      var gate = gates.find(e.gateId()).orElse(null);
      if (gate == null) {
        results.add(new OfflineEntryResult(e.attemptId(), OfflineOutcome.REJECTED, "unknown gate"));
        continue;
      }
      forwarded.add(
          new OfflineItem(
              e.attemptId(),
              e.journalSeq(),
              e.employeeUuid(),
              gate.gateId(),
              gate.factoryCode(),
              e.direction(),
              e.jti(),
              e.scannedAt(),
              e.localStatus(),
              e.deniedReason()));
    }
    if (!forwarded.isEmpty()) {
      for (var r : attendance.ingestOffline(kioskId, forwarded)) {
        results.add(
            new OfflineEntryResult(
                r.attemptId(), OfflineOutcome.valueOf(r.outcome().name()), r.detail()));
      }
    }
    return results;
  }
}
