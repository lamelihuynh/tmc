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

public class SyncAttendanceEventUseCase {
  private final ISyncLogRepository log;
  private final IHrPayrollPort hr;
  private final IClock clock;

  public SyncAttendanceEventUseCase(ISyncLogRepository log, IHrPayrollPort hr, IClock clock) {
    this.log = log;
    this.hr = hr;
    this.clock = clock;
  }

  public synchronized void handle(EventEnvelope event) {
    boolean corrected = "AttendanceCorrected".equals(event.eventType());
    if (!corrected && !"AttendanceConfirmed".equals(event.eventType())) {
      return;
    }

    var previous = log.find(event.eventId()).orElse(null);
    long version = event.aggregateVersion();

    if (previous != null && version < previous.aggregateVersion()) {
      return;
    }

    UUID batchId = event.eventId();
    if (corrected) {
      var originalId = UUID.fromString(requiredField(event, "originalAttemptId"));
      if (!originalId.equals(event.eventId())) {
        throw new IllegalArgumentException("originalAttemptId does not match eventId");
      }
      batchId = UUID.fromString(requiredField(event, "correctionId"));
    }

    SyncRecord record;
    if (previous != null && version == previous.aggregateVersion()) {
      if (!previous.batchId().equals(batchId)) {
        throw new IllegalArgumentException("conflicting batch IDs for the same attendance version");
      }
      if (previous.isSettled() || resolveBeforeRetry(previous)) {
        return;
      }
      record = previous;
    } else {
      if (previous != null && isUncertain(previous)) {
        switch (hr.lookup(previous.batchId())) {
          case UNKNOWN ->
              throw new IllegalStateException(
                  "previous HR/Payroll batch is still unresolved: " + previous.batchId());
          case ACCEPTED -> log.save(previous.with(SyncStatus.SYNCED, null, clock.now()));
          case REJECTED ->
              log.save(previous.with(SyncStatus.FAILED, "rejected by HR/Payroll", clock.now()));
          case NOT_FOUND -> {}
        }
      }

      var scannedAt = Instant.parse(requiredField(event, "scannedAt"));
      var period =
          java.time.YearMonth.from(scannedAt.atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));

      record =
          new SyncRecord(
              event.eventId(), SyncStatus.PENDING, 0, null, clock.now(), version, batchId, period);
    }

    var submission =
        new Submission(
            event.eventId(),
            batchId,
            UUID.fromString(requiredField(event, "employeeUuid")),
            event.factoryCode(),
            requiredField(event, "direction"),
            Instant.parse(requiredField(event, "scannedAt")));

    record = record.with(SyncStatus.IN_PROGRESS, null, clock.now());
    log.save(record);

    var result = hr.submit(submission);
    var settled =
        switch (result) {
          case ACCEPTED -> record.with(SyncStatus.SYNCED, null, clock.now());
          case REJECTED -> record.with(SyncStatus.FAILED, "rejected by HR/Payroll", clock.now());
          case TIMEOUT -> record.with(SyncStatus.UNKNOWN, "no answer from HR/Payroll", clock.now());
        };
    log.save(settled);
  }

  private boolean resolveBeforeRetry(SyncRecord record) {
    if (!isUncertain(record)) {
      return false;
    }

    switch (hr.lookup(record.batchId())) {
      case ACCEPTED -> log.save(record.with(SyncStatus.SYNCED, null, clock.now()));
      case REJECTED ->
          log.save(record.with(SyncStatus.FAILED, "rejected by HR/Payroll", clock.now()));
      case UNKNOWN ->
          log.save(record.with(SyncStatus.UNKNOWN, "no answer from HR/Payroll", clock.now()));
      case NOT_FOUND -> {
        return false;
      }
    }
    return true;
  }

  private static boolean isUncertain(SyncRecord record) {
    return record.status() == SyncStatus.UNKNOWN || record.status() == SyncStatus.IN_PROGRESS;
  }

  private static String requiredField(EventEnvelope event, String name) {
    var value = event.payload().get(name);
    if (value == null) {
      throw new IllegalArgumentException("missing payroll field: " + name);
    }
    return value.toString();
  }
}
