package vn.tmc.hrintegration.application.usecase;

import java.time.Instant;
import java.util.UUID;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort.Submission;
import vn.tmc.hrintegration.application.port.out.ISyncLogRepository;
import vn.tmc.hrintegration.domain.SyncRecord;
import vn.tmc.hrintegration.domain.SyncStatus;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.IClock;

/**
 * Per-event path of document.md 13.2 phase 1. Runs OFF the kiosk path: HR being slow or down never
 * affects a scan (ADR-07). Rules: a settled event is never sent again; a rejection is not retried
 * blindly; a timeout becomes UNKNOWN, not FAILED, because HR may have stored it. TODO Team D:
 * ResolveUnknown (look the result up before resending), retry scheduler with backoff and DLQ,
 * period reconciliation and the 08:00 / 12:00 deadline alerts (ADR-07, NFR-08).
 */
public class SyncAttendanceEventUseCase {
  private final ISyncLogRepository log;
  private final IHrPayrollPort hr;
  private final IClock clock;

  public SyncAttendanceEventUseCase(ISyncLogRepository log, IHrPayrollPort hr, IClock clock) {
    this.log = log;
    this.hr = hr;
    this.clock = clock;
  }

  public void handle(EventEnvelope event) {
    if (!"AttendanceConfirmed".equals(event.eventType())) {
      return; // GateResultRecorded and others are not payroll data
    }
    var existing = log.find(event.eventId());
    if (existing.isPresent() && existing.get().isSettled()) {
      return;
    }
    var now = clock.now();
    var record =
        existing
            .orElse(new SyncRecord(event.eventId(), SyncStatus.PENDING, 0, null, now))
            .with(SyncStatus.IN_PROGRESS, null, now);
    log.save(record);

    var employee = event.payload().get("employeeUuid");
    var direction = event.payload().get("direction");
    var result =
        hr.submit(
            new Submission(
                event.eventId(),
                UUID.fromString(String.valueOf(employee)),
                event.factoryCode(),
                String.valueOf(direction),
                Instant.parse(String.valueOf(event.payload().get("scannedAt")))));

    var settled =
        switch (result) {
          case ACCEPTED -> record.with(SyncStatus.SYNCED, null, clock.now());
          case REJECTED -> record.with(SyncStatus.FAILED, "rejected by HR/Payroll", clock.now());
          case TIMEOUT -> record.with(SyncStatus.UNKNOWN, "no answer from HR/Payroll", clock.now());
        };
    log.save(settled);
  }
}
