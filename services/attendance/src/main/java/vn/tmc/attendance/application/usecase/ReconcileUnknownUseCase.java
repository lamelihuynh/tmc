package vn.tmc.attendance.application.usecase;

import vn.tmc.attendance.application.port.in.GateResult;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.application.port.out.IGateStatusQuery;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.application.port.out.ITransactionRunner;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Resolves GATE_UNKNOWN by asking what the gate really did (ADR-11 P3). Opened means the person
 * passed and the attendance must count. Not opened means the attempt fails. Still undetermined
 * stays for the guard (UC-06).
 */
public class ReconcileUnknownUseCase {
  private final IAttemptRepository attempts;
  private final IGateStatusQuery gateStatus;
  private final ITransactionRunner tx;
  private final GateResultApplier applier;
  private final int batchSize;

  public ReconcileUnknownUseCase(
      IAttemptRepository attempts,
      IAttendanceTransactionRepository transactions,
      IOutboxRepository outbox,
      IGateStatusQuery gateStatus,
      ITransactionRunner tx,
      AttendanceEventFactory events,
      int batchSize,
      IClock clock) {
    this.attempts = attempts;
    this.gateStatus = gateStatus;
    this.tx = tx;
    this.batchSize = batchSize;
    this.applier = new GateResultApplier(attempts, transactions, outbox, events, clock);
  }

  public record Report(int committed, int failed, int stillUnknown) {}

  public Report run() {
    int committed = 0;
    int failed = 0;
    int still = 0;
    var unknown = attempts.findByStatus(AttemptStatus.GATE_UNKNOWN, batchSize);
    for (var attempt : unknown) {
      var observation = gateStatus.observe(attempt);
      switch (observation) {
        case OPENED -> {
          tx.inTransaction(
              () -> {
                applier.apply(attempt, GateResult.OPENED, null, "reconcile");
                return 0;
              });
          committed++;
        }
        case NOT_OPENED -> {
          tx.inTransaction(
              () -> {
                applier.apply(
                    attempt,
                    GateResult.ERROR,
                    ReasonCode.TURNSTILE_TIMEOUT_RETRY_SCAN,
                    "reconcile");
                return 0;
              });
          failed++;
        }
        case UNDETERMINED -> still++;
      }
    }
    return new Report(committed, failed, still);
  }
}
