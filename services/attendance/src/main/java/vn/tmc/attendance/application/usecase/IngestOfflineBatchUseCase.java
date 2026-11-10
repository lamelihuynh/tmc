package vn.tmc.attendance.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import vn.tmc.attendance.application.port.in.IOfflineAttendanceIngest;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.application.port.out.ITransactionRunner;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.Anomaly;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.AttendanceTransaction;
import vn.tmc.platform.kernel.IClock;

/**
 * ADR-11 section 5. Records are processed in journalSeq order, each in its own transaction so one
 * bad record never blocks the rest. This use case NEVER sends an open command: the gate already
 * opened.
 */
public class IngestOfflineBatchUseCase implements IOfflineAttendanceIngest {
  private final IAttemptRepository attempts;
  private final IAttendanceTransactionRepository transactions;
  private final IOutboxRepository outbox;
  private final ITransactionRunner tx;
  private final AttendanceEventFactory events;
  private final IClock clock;

  public IngestOfflineBatchUseCase(
      IAttemptRepository attempts,
      IAttendanceTransactionRepository transactions,
      IOutboxRepository outbox,
      ITransactionRunner tx,
      AttendanceEventFactory events,
      IClock clock) {
    this.attempts = attempts;
    this.transactions = transactions;
    this.outbox = outbox;
    this.tx = tx;
    this.events = events;
    this.clock = clock;
  }

  @Override
  public List<OfflineResult> ingest(String kioskId, List<OfflineRecord> records) {
    var ordered = new ArrayList<>(records);
    ordered.sort(Comparator.comparingLong(OfflineRecord::journalSeq));
    var results = new ArrayList<OfflineResult>(ordered.size());
    for (var record : ordered) {
      results.add(tx.inTransaction(() -> ingestOne(kioskId, record)));
    }
    return results;
  }

  private OfflineResult ingestOne(String kioskId, OfflineRecord r) {
    // step 1: duplicate attempt_id
    if (attempts.findById(r.attemptId()).isPresent()) {
      return new OfflineResult(r.attemptId(), Outcome.DUPLICATE, "attempt already known");
    }
    var now = clock.now();
    var correlation = "offline-" + kioskId;
    var attempt =
        AccessAttempt.validated(
            r.attemptId(),
            r.employeeUuid(),
            r.gateId(),
            r.factoryCode(),
            r.direction(),
            r.jti(),
            AttemptSource.OFFLINE,
            r.scannedAt(),
            now);

    if (r.localStatus() == LocalStatus.OFFLINE_DENIED) {
      attempt.deny(r.deniedReason(), now);
      attempts.save(attempt);
      outbox.add(events.auditRecorded(attempt, "OFFLINE_ACCESS_DENIED", correlation));
      return new OfflineResult(r.attemptId(), Outcome.ACCEPTED, "denial recorded");
    }

    // steps 2 and 3: replay between kiosks, then anti-passback at the scan time
    Anomaly anomaly = null;
    if (r.jti() != null && attempts.existsOtherWithJti(r.jti(), r.attemptId())) {
      anomaly = Anomaly.TOKEN_REUSED_OFFLINE;
    } else if (r.employeeUuid() != null
        && transactions
            .lastDirectionBefore(r.employeeUuid(), r.scannedAt())
            .filter(last -> last == r.direction())
            .isPresent()) {
      anomaly = Anomaly.ANTI_PASSBACK_OFFLINE;
    }

    // the gate physically opened already, so the aggregate walks to COMMITTED through its guarded
    // transitions
    attempt.reserve(now);
    attempt.commandGate(now);
    attempt.gateOpened(now);
    attempt.commit(now);
    attempt.flagAnomaly(anomaly);
    attempts.save(attempt);

    if (anomaly == null) {
      // step 4: counted as attendance
      transactions.saveIfAbsent(
          new AttendanceTransaction(
              attempt.attemptId(),
              attempt.employeeUuid(),
              attempt.factoryCode(),
              attempt.gateId(),
              attempt.direction(),
              attempt.scannedAt(),
              AttemptSource.OFFLINE));
      outbox.add(events.attendanceConfirmed(attempt, correlation));
      return new OfflineResult(r.attemptId(), Outcome.ACCEPTED, "counted");
    }
    // anomaly: keep the record, do not count it, alert via audit (HR resolves in UC-09)
    outbox.add(events.auditRecorded(attempt, anomaly.name(), correlation));
    return new OfflineResult(r.attemptId(), Outcome.ANOMALY, anomaly.name());
  }
}
