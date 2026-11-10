package vn.tmc.reporting.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.InMemoryProcessedMessageStore;
import vn.tmc.platform.messaging.ProcessedMessageStore;
import vn.tmc.reporting.adapter.out.inmem.InMemoryPresenceRepository;
import vn.tmc.reporting.application.port.in.IReporting;
import vn.tmc.reporting.application.port.out.IPresenceRepository;
import vn.tmc.reporting.application.usecase.ProjectAttendanceEventUseCase;
import vn.tmc.reporting.application.usecase.ReportingQueryService;

@Configuration
public class ReportingConfig {
  @Bean
  IPresenceRepository presenceRepository() {
    return new InMemoryPresenceRepository();
  }

  /** TODO Team D: JDBC store (table processed_messages) shared transaction with the read model. */
  @Bean
  ProcessedMessageStore processedMessageStore() {
    return new InMemoryProcessedMessageStore();
  }

  @Bean
  ProjectAttendanceEventUseCase projectAttendanceEvent(IPresenceRepository presences) {
    return new ProjectAttendanceEventUseCase(presences);
  }

  @Bean
  IdempotentConsumer reportingConsumer(
      ProcessedMessageStore store, ProjectAttendanceEventUseCase projection) {
    return new IdempotentConsumer("reporting", store, projection::handle);
  }

  @Bean
  IReporting reporting(IPresenceRepository presences) {
    return new ReportingQueryService(presences);
  }
}
