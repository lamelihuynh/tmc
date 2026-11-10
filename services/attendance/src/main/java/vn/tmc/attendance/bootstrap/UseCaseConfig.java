package vn.tmc.attendance.bootstrap;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.tmc.attendance.application.port.in.IAttendanceCommand;
import vn.tmc.attendance.application.port.in.IAttendanceQuery;
import vn.tmc.attendance.application.port.in.IOfflineAttendanceIngest;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.application.port.out.IGateStatusQuery;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.application.port.out.ITransactionRunner;
import vn.tmc.attendance.application.usecase.AttendanceCommandService;
import vn.tmc.attendance.application.usecase.AttendanceEventFactory;
import vn.tmc.attendance.application.usecase.AttendanceQueryService;
import vn.tmc.attendance.application.usecase.IngestOfflineBatchUseCase;
import vn.tmc.attendance.application.usecase.PublishOutboxUseCase;
import vn.tmc.attendance.application.usecase.ReconcileUnknownUseCase;
import vn.tmc.attendance.application.usecase.RecoverStaleAttemptsUseCase;
import vn.tmc.attendance.application.usecase.RecoveryPolicy;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.messaging.EventPublisher;

/**
 * Wires use cases to whatever ports the active profile provides. Business classes carry no Spring
 * annotations; this is the only place that knows the composition (DP-10 Configuration over Code).
 */
@Configuration
public class UseCaseConfig {

  @Bean
  IClock clock() {
    return IClock.system();
  }

  @Bean
  AttendanceEventFactory eventFactory(IClock clock) {
    return new AttendanceEventFactory(clock);
  }

  @Bean
  RecoveryPolicy recoveryPolicy(
      @Value("${tmc.attendance.recovery.reserved-ttl-seconds:30}") long reservedTtl,
      @Value("${tmc.attendance.recovery.command-grace-seconds:10}") long commandGrace,
      @Value("${tmc.attendance.batch-size:100}") int batchSize) {
    return new RecoveryPolicy(
        Duration.ofSeconds(reservedTtl), Duration.ofSeconds(commandGrace), batchSize);
  }

  @Bean
  IAttendanceCommand attendanceCommand(
      IAttemptRepository attempts,
      IAttendanceTransactionRepository transactions,
      IOutboxRepository outbox,
      ITransactionRunner tx,
      AttendanceEventFactory events,
      IClock clock) {
    return new AttendanceCommandService(attempts, transactions, outbox, tx, events, clock);
  }

  @Bean
  IAttendanceQuery attendanceQuery(IAttemptRepository attempts) {
    return new AttendanceQueryService(attempts);
  }

  @Bean
  IOfflineAttendanceIngest offlineIngest(
      IAttemptRepository attempts,
      IAttendanceTransactionRepository transactions,
      IOutboxRepository outbox,
      ITransactionRunner tx,
      AttendanceEventFactory events,
      IClock clock) {
    return new IngestOfflineBatchUseCase(attempts, transactions, outbox, tx, events, clock);
  }

  @Bean
  RecoverStaleAttemptsUseCase recoverStale(
      IAttemptRepository attempts,
      IAttendanceTransactionRepository transactions,
      IOutboxRepository outbox,
      ITransactionRunner tx,
      AttendanceEventFactory events,
      RecoveryPolicy policy,
      IClock clock) {
    return new RecoverStaleAttemptsUseCase(
        attempts, transactions, outbox, tx, events, policy, clock);
  }

  @Bean
  ReconcileUnknownUseCase reconcileUnknown(
      IAttemptRepository attempts,
      IAttendanceTransactionRepository transactions,
      IOutboxRepository outbox,
      IGateStatusQuery gateStatus,
      ITransactionRunner tx,
      AttendanceEventFactory events,
      RecoveryPolicy policy,
      IClock clock) {
    return new ReconcileUnknownUseCase(
        attempts, transactions, outbox, gateStatus, tx, events, policy.batchSize(), clock);
  }

  @Bean
  PublishOutboxUseCase publishOutbox(
      IOutboxRepository outbox, EventPublisher publisher, RecoveryPolicy policy) {
    return new PublishOutboxUseCase(outbox, publisher, policy.batchSize());
  }
}
