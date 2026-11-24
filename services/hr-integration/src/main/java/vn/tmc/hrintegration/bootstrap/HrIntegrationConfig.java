package vn.tmc.hrintegration.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import vn.tmc.hrintegration.adapter.out.http.HttpHrPayrollAdapter;
import vn.tmc.hrintegration.adapter.out.inmem.InMemorySyncLogRepository;
import vn.tmc.hrintegration.adapter.out.inmem.SimulatedHrPayrollAdapter;
import vn.tmc.hrintegration.application.port.in.ISyncMonitoring;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort;
import vn.tmc.hrintegration.application.port.out.ISyncLogRepository;
import vn.tmc.hrintegration.application.usecase.SyncAttendanceEventUseCase;
import vn.tmc.hrintegration.application.usecase.SyncMonitoringService;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.ProcessedMessageStore;

@Configuration
@Import(DedupStoreConfig.class)
public class HrIntegrationConfig {
  @Bean
  IClock clock() {
    return IClock.system();
  }

  @Bean
  ISyncLogRepository syncLog() {
    return new InMemorySyncLogRepository();
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
}