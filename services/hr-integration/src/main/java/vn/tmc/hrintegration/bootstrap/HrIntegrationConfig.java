package vn.tmc.hrintegration.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.tmc.hrintegration.adapter.out.inmem.InMemorySyncLogRepository;
import vn.tmc.hrintegration.adapter.out.inmem.SimulatedHrPayrollAdapter;
import vn.tmc.hrintegration.application.port.in.ISyncMonitoring;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort;
import vn.tmc.hrintegration.application.port.out.ISyncLogRepository;
import vn.tmc.hrintegration.application.usecase.SyncAttendanceEventUseCase;
import vn.tmc.hrintegration.application.usecase.SyncMonitoringService;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.InMemoryProcessedMessageStore;
import vn.tmc.platform.messaging.ProcessedMessageStore;

@Configuration
public class HrIntegrationConfig {
  @Bean
  IClock clock() {
    return IClock.system();
  }

  @Bean
  ISyncLogRepository syncLog() {
    return new InMemorySyncLogRepository();
  }

  /** TODO Team D: replace with the HTTP adapter for the real HR/Payroll API (document.md 16.4). */
  @Bean
  IHrPayrollPort hrPayroll() {
    return new SimulatedHrPayrollAdapter();
  }

  @Bean
  ProcessedMessageStore processedMessageStore() {
    return new InMemoryProcessedMessageStore();
  }

  @Bean
  SyncAttendanceEventUseCase syncAttendanceEvent(
      ISyncLogRepository log, IHrPayrollPort hr, IClock clock) {
    return new SyncAttendanceEventUseCase(log, hr, clock);
  }

  @Bean
  IdempotentConsumer hrConsumer(ProcessedMessageStore store, SyncAttendanceEventUseCase sync) {
    return new IdempotentConsumer("hr-integration", store, sync::handle);
  }

  @Bean
  ISyncMonitoring syncMonitoring(ISyncLogRepository log) {
    return new SyncMonitoringService(log);
  }
}
