package vn.tmc.hrintegration.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import vn.tmc.hrintegration.application.usecase.SyncAttendanceEventUseCase;

/**
 * Retries HR/Payroll batches without waiting for a redelivery: UNKNOWN rows are looked up and
 * resent only if HR does not have them, RETRY_PENDING rows are resent, IN_PROGRESS rows left by a
 * stopped process are treated as UNKNOWN. Backoff per row is in SyncRecord.retryLater. Off with
 * {@code tmc.hr.reconcile-enabled=false}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "tmc.hr.reconcile-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class HrReconcileConfig {
  @Bean
  ReconcileJob hrReconcileJob(SyncAttendanceEventUseCase sync) {
    return new ReconcileJob(sync);
  }

  public static class ReconcileJob {
    private static final Logger LOG = LoggerFactory.getLogger(ReconcileJob.class);
    private static final int BATCH = 100;
    private final SyncAttendanceEventUseCase sync;

    public ReconcileJob(SyncAttendanceEventUseCase sync) {
      this.sync = sync;
    }

    @Scheduled(
        fixedDelayString = "${tmc.hr.reconcile-interval:PT1M}",
        initialDelayString = "${tmc.hr.reconcile-interval:PT1M}")
    public void run() {
      try {
        int n = sync.retryDue(BATCH);
        if (n > 0) {
          LOG.info("HR reconcile: retried {} row(s)", n);
        }
      } catch (RuntimeException e) {
        LOG.warn("HR reconcile run failed: {}", e.toString());
      }
    }
  }
}
