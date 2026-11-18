package vn.tmc.reporting.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.ProcessedMessageStore;
import vn.tmc.reporting.adapter.out.inmem.InMemoryPresenceRepository;
import vn.tmc.reporting.application.port.in.IReporting;
import vn.tmc.reporting.application.port.out.IPresenceRepository;
import vn.tmc.reporting.application.usecase.ProjectAttendanceEventUseCase;
import vn.tmc.reporting.application.usecase.ReportingQueryService;

@Configuration
@Import(DedupStoreConfig.class)
public class ReportingConfig {
  @Bean
  IPresenceRepository presenceRepository() {
    return new InMemoryPresenceRepository();
  }

  @Bean
  ProjectAttendanceEventUseCase projectAttendanceEvent(IPresenceRepository presences) {
    return new ProjectAttendanceEventUseCase(presences);
  }

  @Bean
  IdempotentConsumer reportingConsumer(
      ProcessedMessageStore store,
      IdempotentConsumer.TransactionRunner tx,
      ProjectAttendanceEventUseCase projection) {
    return new IdempotentConsumer("reporting", store, projection::handle, tx);
  }

  @Bean
  IReporting reporting(IPresenceRepository presences) {
    return new ReportingQueryService(presences);
  }
}
