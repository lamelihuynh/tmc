package vn.tmc.gatestation.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import vn.tmc.gatestation.application.usecase.SyncEmployeeDirectoryUseCase;

/**
 * Pulls employee eligibility from User/QR every {@code tmc.gate.employee-sync.interval-ms}. Off on
 * the inmem profile (no User/QR) and when {@code tmc.gate.employee-sync.enabled=false}. A failed
 * pull is logged and the directory keeps its last known content.
 */
@Configuration
@EnableScheduling
@Profile("!inmem")
@ConditionalOnProperty(
    name = "tmc.gate.employee-sync.enabled",
    havingValue = "true",
    matchIfMissing = true)
class EmployeeDirectorySyncJob {
  private static final Logger LOG = LoggerFactory.getLogger(EmployeeDirectorySyncJob.class);
  private final SyncEmployeeDirectoryUseCase sync;

  EmployeeDirectorySyncJob(SyncEmployeeDirectoryUseCase sync) {
    this.sync = sync;
  }

  @Scheduled(
      initialDelayString = "${tmc.gate.employee-sync.initial-delay-ms:5000}",
      fixedDelayString = "${tmc.gate.employee-sync.interval-ms:30000}")
  void refresh() {
    try {
      int changed = sync.refresh();
      if (changed > 0) {
        LOG.info("employee directory: {} employees updated from the sync bundle", changed);
      }
    } catch (RuntimeException e) {
      LOG.warn("employee directory sync failed, keeping the last known list: {}", e.toString());
    }
  }
}
