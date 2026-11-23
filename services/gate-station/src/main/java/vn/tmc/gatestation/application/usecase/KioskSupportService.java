package vn.tmc.gatestation.application.usecase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import vn.tmc.gatestation.application.port.in.IKioskSupport;
import vn.tmc.gatestation.application.port.out.IAntiPassbackStore;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.OfflineItem;
import vn.tmc.gatestation.application.port.out.IGateConfigRepository;
import vn.tmc.gatestation.application.port.out.IHeartbeatStore;
import vn.tmc.gatestation.application.port.out.IKioskSyncSource;
import vn.tmc.gatestation.domain.LastPass;
import vn.tmc.platform.kernel.IClock;

public class KioskSupportService implements IKioskSupport {
  private final IGateConfigRepository gates;
  private final IKioskSyncSource source;
  private final IHeartbeatStore heartbeats;
  private final IAttendanceGateway attendance;
  private final IAntiPassbackStore antiPassback;
  private final IClock clock;

  public KioskSupportService(
      IGateConfigRepository gates,
      IKioskSyncSource source,
      IHeartbeatStore heartbeats,
      IAttendanceGateway attendance,
      IAntiPassbackStore antiPassback,
      IClock clock) {
    this.attendance = attendance;
    this.antiPassback = antiPassback;
    this.gates = gates;
    this.source = source;
    this.heartbeats = heartbeats;
    this.clock = clock;
  }

  @Override
  public SyncBundle syncBundle(Instant since) {
    var delta = source.changedSince(since);
    var employees =
        delta.employees().stream()
            .map(
                e ->
                    new EmployeeEligibility(
                        e.employeeUuid().toString(),
                        e.active() ? "ACTIVE" : "INACTIVE",
                        e.allowedFactories().stream().sorted().toList()))
            .toList();
    return new SyncBundle(clock.now(), gates.all(), delta.deviceKeys(), employees);
  }

  @Override
  public void heartbeat(Heartbeat heartbeat) {
    heartbeats.record(heartbeat, clock.now());
  }

  @Override
  public List<OfflineEntryResult> ingestOffline(String kioskId, List<OfflineEntry> entries) {
    var results = new ArrayList<OfflineEntryResult>();
    var forwarded = new ArrayList<OfflineItem>();
    Map<UUID, OfflineItem> byAttempt = new HashMap<>();
    for (var e : entries) {
      var gate = gates.find(e.gateId()).orElse(null);
      if (gate == null) {
        results.add(new OfflineEntryResult(e.attemptId(), OfflineOutcome.REJECTED, "unknown gate"));
        continue;
      }
      var item =
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
              e.deniedReason());
      forwarded.add(item);
      byAttempt.putIfAbsent(item.attemptId(), item);
    }
    if (!forwarded.isEmpty()) {
      for (var r : attendance.ingestOffline(kioskId, forwarded)) {
        var item = byAttempt.get(r.attemptId());
        // Attendance also returns ACCEPTED for an offline denial. Only a counted physical pass
        // changes anti-passback, and an older journal entry must not replace a newer pass.
        if (r.outcome() == IAttendanceGateway.OfflineOutcome.ACCEPTED
            && item != null
            && "OFFLINE_OPENED".equals(item.localStatus())
            && item.employeeUuid() != null
            && item.direction() != null) {
          antiPassback.rememberIfNewer(
              item.employeeUuid(), new LastPass(item.direction(), item.scannedAt()));
        }
        results.add(
            new OfflineEntryResult(
                r.attemptId(), OfflineOutcome.valueOf(r.outcome().name()), r.detail()));
      }
    }
    return results;
  }
}
