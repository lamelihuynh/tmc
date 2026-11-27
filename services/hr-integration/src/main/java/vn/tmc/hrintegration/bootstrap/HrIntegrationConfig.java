package vn.tmc.hrintegration.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.tmc.hrintegration.adapter.out.http.HttpHrPayrollAdapter;
import vn.tmc.hrintegration.adapter.out.inmem.InMemorySyncLogRepository;
import vn.tmc.hrintegration.adapter.out.inmem.SimulatedHrPayrollAdapter;
import vn.tmc.hrintegration.adapter.out.persistence.JdbcSyncLogRepository;
import vn.tmc.hrintegration.application.port.in.ISyncMonitoring;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort;
import vn.tmc.hrintegration.application.port.out.ISyncLogRepository;
import vn.tmc.hrintegration.application.usecase.SyncAttendanceEventUseCase;
import vn.tmc.hrintegration.application.usecase.SyncMonitoringService;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.ProcessedMessageStore;

/**
 * HR/Payroll mode {@code tmc.hr.mode}: {@code simulated} (the default, there is no HR system in
 * this project) or {@code http} with {@code tmc.hr.url} and {@code HR_PAYROLL_API_TOKEN}. Any other
 * value stops the start-up instead of silently running without an adapter.
 */
@Configuration
@Import(DedupStoreConfig.class)
public class HrIntegrationConfig {
  private static final Logger LOG = LoggerFactory.getLogger(HrIntegrationConfig.class);

  @Bean
  IClock clock() {
    return IClock.system();
  }

  @Bean
  HrModeCheck hrModeCheck(@Value("${tmc.hr.mode:simulated}") String mode) {
    return new HrModeCheck(mode);
  }

  /** Fails the start-up on an unknown mode; warns when nothing reaches a real HR system. */
  public static final class HrModeCheck {
    HrModeCheck(String mode) {
      switch (mode) {
        case "simulated" ->
            LOG.warn(
                "tmc.hr.mode=simulated: attendance is NOT sent to a real HR/Payroll system."
                    + " Set tmc.hr.mode=http, tmc.hr.url and HR_PAYROLL_API_TOKEN to send it.");
        case "http" -> LOG.info("tmc.hr.mode=http");
        default ->
            throw new IllegalStateException(
                "tmc.hr.mode must be 'simulated' or 'http' (lower case), got '" + mode + "'");
      }
    }
  }

  @Bean
  @ConditionalOnProperty(name = "tmc.hr.mode", havingValue = "simulated", matchIfMissing = true)
  SimulatedHrPayrollAdapter hrPayroll() {
    return new SimulatedHrPayrollAdapter();
  }

  @Bean
  @ConditionalOnProperty(name = "tmc.hr.mode", havingValue = "http")
  HttpHrPayrollAdapter httpHrPayroll(
      @Value("${tmc.hr.url}") URI baseUri,
      @Value("${HR_PAYROLL_API_TOKEN}") String bearerToken,
      ObjectMapper json) {
    if (!baseUri.isAbsolute()) {
      throw new IllegalStateException("tmc.hr.mode=http needs tmc.hr.url (HR_PAYROLL_URL)");
    }
    if (bearerToken.isBlank()) {
      throw new IllegalStateException("tmc.hr.mode=http needs HR_PAYROLL_API_TOKEN");
    }
    return new HttpHrPayrollAdapter(baseUri, bearerToken, json);
  }

  @Bean
  SyncAttendanceEventUseCase syncAttendanceEvent(
      ISyncLogRepository log, IHrPayrollPort hr, IClock clock) {
    return new SyncAttendanceEventUseCase(log, hr, clock);
  }

  @Bean
  IdempotentConsumer hrConsumer(
      ProcessedMessageStore store,
      IdempotentConsumer.TransactionRunner tx,
      SyncAttendanceEventUseCase sync) {
    return new IdempotentConsumer("hr-integration", store, sync::handle, tx);
  }

  @Bean
  ISyncMonitoring syncMonitoring(ISyncLogRepository log) {
    return new SyncMonitoringService(log);
  }

  /** No database: the sync log lives in process memory and is lost on restart. */
  @Configuration
  @Profile({"inmem", "local"})
  static class InMemory {
    @Bean
    ISyncLogRepository syncLog() {
      return new InMemorySyncLogRepository();
    }
  }

  /** PostgreSQL: table sync_log (Flyway V2), in the dedup transaction of the listener. */
  @Configuration
  @Profile("!inmem & !local")
  static class Postgres {
    @Bean
    ISyncLogRepository syncLog(JdbcTemplate jdbc) {
      return new JdbcSyncLogRepository(jdbc);
    }
  }
}
