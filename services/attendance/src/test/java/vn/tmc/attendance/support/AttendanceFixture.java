package vn.tmc.attendance.support;

import java.time.Duration;
import java.util.UUID;
import vn.tmc.attendance.adapter.out.inmem.ConfigurableGateStatusQuery;
import vn.tmc.attendance.adapter.out.inmem.InMemoryAttemptRepository;
import vn.tmc.attendance.adapter.out.inmem.InMemoryAttendanceTransactionRepository;
import vn.tmc.attendance.adapter.out.inmem.InMemoryOutboxRepository;
import vn.tmc.attendance.adapter.out.inmem.InMemoryStore;
import vn.tmc.attendance.application.port.in.IAttendanceCommand.RecordAttemptCommand;
import vn.tmc.attendance.application.usecase.AttendanceCommandService;
import vn.tmc.attendance.application.usecase.AttendanceEventFactory;
import vn.tmc.attendance.application.usecase.IngestOfflineBatchUseCase;
import vn.tmc.attendance.application.usecase.PublishOutboxUseCase;
import vn.tmc.attendance.application.usecase.ReconcileUnknownUseCase;
import vn.tmc.attendance.application.usecase.RecoverStaleAttemptsUseCase;
import vn.tmc.attendance.application.usecase.RecoveryPolicy;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.messaging.InMemoryEventBroker;

/** Wires the whole attendance application on in-memory adapters, no Spring involved. */
public class AttendanceFixture {
  public final MutableClock clock = new MutableClock();
  public final InMemoryStore store = new InMemoryStore();
  public final InMemoryAttemptRepository attempts = new InMemoryAttemptRepository();
  public final InMemoryAttendanceTransactionRepository transactions =
      new InMemoryAttendanceTransactionRepository();
  public final InMemoryOutboxRepository outbox = new InMemoryOutboxRepository();
  public final InMemoryEventBroker broker = new InMemoryEventBroker();
  public final ConfigurableGateStatusQuery gateStatus = new ConfigurableGateStatusQuery();
  public final AttendanceEventFactory events = new AttendanceEventFactory(clock);
  public final RecoveryPolicy policy =
      new RecoveryPolicy(Duration.ofSeconds(30), Duration.ofSeconds(10), 100);

  public final AttendanceCommandService command =
      new AttendanceCommandService(attempts, transactions, outbox, store, events, clock);
  public final IngestOfflineBatchUseCase offline =
      new IngestOfflineBatchUseCase(attempts, transactions, outbox, store, events, clock);
  public final RecoverStaleAttemptsUseCase recovery =
      new RecoverStaleAttemptsUseCase(attempts, transactions, outbox, store, events, policy, clock);
  public final ReconcileUnknownUseCase reconcile =
      new ReconcileUnknownUseCase(
          attempts, transactions, outbox, gateStatus, store, events, 100, clock);
  public final PublishOutboxUseCase publisher = new PublishOutboxUseCase(outbox, broker, 100);

  public static final UUID EMPLOYEE = UUID.fromString("0192a1c0-0000-7000-8000-000000000001");

  public RecordAttemptCommand attemptCommand(UUID attemptId, String jti, Direction direction) {
    return new RecordAttemptCommand(
        attemptId,
        EMPLOYEE,
        "BD-GATE-01",
        "BD",
        direction,
        jti,
        AttemptSource.ONLINE,
        clock.now(),
        "corr-1");
  }
}
