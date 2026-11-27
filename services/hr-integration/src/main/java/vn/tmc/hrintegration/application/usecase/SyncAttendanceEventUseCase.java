package vn.tmc.hrintegration.application.usecase;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort.Submission;
import vn.tmc.hrintegration.application.port.out.ISyncLogRepository;
import vn.tmc.hrintegration.domain.PayrollLine;
import vn.tmc.hrintegration.domain.SyncRecord;
import vn.tmc.hrintegration.domain.SyncStatus;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.IClock;

/**
 * Sends AttendanceConfirmed and AttendanceCorrected to HR/Payroll as one-line batches (ADR-07,
 * D-06) and keeps the sync log. Rules:
 *
 * <ul>
 *   <li>An event older than the logged version of the same attempt is ignored.
 *   <li>A batch whose outcome is uncertain (UNKNOWN, or IN_PROGRESS after a crash) is looked up at
 *       HR before it is ever resent; only NOT_FOUND allows a resend, with the same Idempotency-Key.
 *   <li>A correction is a new batch (event_id = correctionId) carrying corrects_event_id = the
 *       attemptId. It is sent whatever the state of the line it corrects: HR applies a correction
 *       over the line it names whatever the arrival order (hr-payroll.openapi.yaml).
 *   <li>Nothing waits on a redelivery: {@link #retryDue} (scheduled) retries UNKNOWN and
 *       RETRY_PENDING rows with backoff, and IN_PROGRESS rows left by a stopped process.
 * </ul>
 */
public class SyncAttendanceEventUseCase {
  private static final Logger LOG = LoggerFactory.getLogger(SyncAttendanceEventUseCase.class);
  private static final ZoneId PAYROLL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  /** An IN_PROGRESS row older than this was left by a process that stopped during the send. */
  public static final Duration STUCK_AFTER = Duration.ofMinutes(2);

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
      return; // an older state of this attempt arrived late
    }

    UUID batchId = event.eventId();
    if (corrected) {
      var originalId = UUID.fromString(requiredField(event, "originalAttemptId"));
      if (!originalId.equals(event.eventId())) {
        throw new IllegalArgumentException("originalAttemptId does not match eventId");
      }
      batchId = UUID.fromString(requiredField(event, "correctionId"));
    }

    if (previous != null && version == previous.aggregateVersion()) {
      if (!previous.batchId().equals(batchId)) {
        throw new IllegalArgumentException("conflicting batch IDs for the same attendance version");
      }
      reconcile(previous); // redelivery: settle or retry what is already logged
      return;
    }

    var scannedAt = Instant.parse(requiredField(event, "scannedAt"));
    var line =
        new PayrollLine(
            UUID.fromString(requiredField(event, "employeeUuid")),
            event.factoryCode(),
            requiredField(event, "direction"),
            scannedAt,
            corrected ? event.eventId() : null);
    send(
        new SyncRecord(
            event.eventId(),
            SyncStatus.PENDING,
            0,
            null,
            clock.now(),
            version,
            batchId,
            YearMonth.from(scannedAt.atZone(PAYROLL_ZONE)),
            line,
            null));
  }

  /**
   * Retries what is due now (called by the scheduled reconcile job).
   *
   * @return number of rows looked at
   */
  public synchronized int retryDue(int limit) {
    var now = clock.now();
    var due = log.dueForRetry(now, now.minus(STUCK_AFTER), limit);
    for (var record : due) {
      try {
        reconcile(record);
      } catch (RuntimeException e) {
        LOG.warn("HR sync retry of {} failed: {}", record.eventId(), e.toString());
        log.save(record.retryLater(record.status(), e.toString(), clock.now()));
      }
    }
    return due.size();
  }

  private void reconcile(SyncRecord record) {
    if (record.isSettled()) {
      return;
    }
    if (record.line() == null) {
      LOG.warn("HR sync row {} has no stored line; it cannot be resent", record.eventId());
      return;
    }
    if (record.isUncertain()) {
      switch (hr.lookup(record.batchId())) {
        case ACCEPTED -> {
          log.save(record.with(SyncStatus.SYNCED, null, clock.now()));
          return;
        }
        case REJECTED -> {
          log.save(record.with(SyncStatus.FAILED, "rejected by HR/Payroll", clock.now()));
          return;
        }
        case UNKNOWN -> {
          log.save(
              record.retryLater(SyncStatus.UNKNOWN, "HR/Payroll lookup inconclusive", clock.now()));
          return;
        }
        case NOT_FOUND -> {} // HR confirms it never got the batch: resend below
      }
    }
    send(record);
  }

  private void send(SyncRecord record) {
    var inProgress = record.with(SyncStatus.IN_PROGRESS, null, clock.now());
    log.save(inProgress);
    var line = inProgress.line();
    var result =
        hr.submit(
            new Submission(
                inProgress.batchId(),
                inProgress.batchId(),
                line.employeeUuid(),
                line.factoryCode(),
                line.direction(),
                line.scannedAt(),
                line.correctsEventId()));
    var now = clock.now();
    log.save(
        switch (result) {
          case ACCEPTED -> inProgress.with(SyncStatus.SYNCED, null, now);
          case REJECTED -> inProgress.with(SyncStatus.FAILED, "rejected by HR/Payroll", now);
          case REFUSED ->
              inProgress.retryLater(
                  SyncStatus.RETRY_PENDING,
                  "HR/Payroll refused the request: check HR_PAYROLL_API_TOKEN and tmc.hr.url",
                  now);
          case TIMEOUT ->
              inProgress.retryLater(SyncStatus.UNKNOWN, "no answer from HR/Payroll", now);
        });
  }

  private static String requiredField(EventEnvelope event, String name) {
    var value = event.payload().get(name);
    if (value == null) {
      throw new IllegalArgumentException("missing payroll field: " + name);
    }
    return value.toString();
  }
}
