package vn.tmc.attendance.adapter.in.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.tmc.attendance.application.usecase.PublishOutboxUseCase;
import vn.tmc.attendance.application.usecase.ReconcileUnknownUseCase;
import vn.tmc.attendance.application.usecase.RecoverStaleAttemptsUseCase;

/**
 * Drives the background jobs of ADR-11 and ADR-04. Off in tests via
 * tmc.attendance.scheduling.enabled=false.
 */
@Component
@ConditionalOnProperty(
    name = "tmc.attendance.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class MaintenanceScheduler {
  private static final Logger log = LoggerFactory.getLogger(MaintenanceScheduler.class);

  private final PublishOutboxUseCase publish;
  private final RecoverStaleAttemptsUseCase recover;
  private final ReconcileUnknownUseCase reconcile;

  public MaintenanceScheduler(
      PublishOutboxUseCase publish,
      RecoverStaleAttemptsUseCase recover,
      ReconcileUnknownUseCase reconcile) {
    this.publish = publish;
    this.recover = recover;
    this.reconcile = reconcile;
  }

  @Scheduled(fixedDelayString = "${tmc.attendance.scheduling.outbox-delay-ms:1000}")
  void publishOutbox() {
    var report = publish.run();
    if (report.stillPending() > 0) {
      log.warn(
          "outbox backlog: published={} pending={}", report.published(), report.stillPending());
    }
  }

  @Scheduled(fixedDelayString = "${tmc.attendance.scheduling.recovery-delay-ms:5000}")
  void recoverStale() {
    var report = recover.run();
    if (report.releasedReservations() + report.movedToUnknown() > 0) {
      log.info("recovery: {}", report);
    }
  }

  @Scheduled(fixedDelayString = "${tmc.attendance.scheduling.reconcile-delay-ms:10000}")
  void reconcileUnknown() {
    var report = reconcile.run();
    if (report.committed() + report.failed() + report.stillUnknown() > 0) {
      log.info("reconcile: {}", report);
    }
  }
}
