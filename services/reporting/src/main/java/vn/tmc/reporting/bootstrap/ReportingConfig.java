package vn.tmc.reporting.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.ProcessedMessageStore;
import vn.tmc.reporting.adapter.out.inmem.InMemoryPresenceRepository;
import vn.tmc.reporting.adapter.out.persistence.JdbcPresenceRepository;
import vn.tmc.reporting.application.port.in.IReporting;
import vn.tmc.reporting.application.port.out.IPresenceRepository;
import vn.tmc.reporting.application.usecase.ProjectAttendanceEventUseCase;
import vn.tmc.reporting.application.usecase.ReportingQueryService;

@Configuration
@Import(DedupStoreConfig.class)
public class ReportingConfig {
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

  /** No database: the read model lives in process memory and is lost on restart. */
  @Configuration
  @Profile({"inmem", "local"})
  static class InMemory {
    @Bean
    IPresenceRepository presenceRepository() {
      return new InMemoryPresenceRepository();
    }
  }

  /**
   * PostgreSQL (D-04): table attendance_projection, written in the dedup transaction set up by
   * DedupStoreConfig, so presence survives a restart and a failed handler leaves no partial state.
   */
  @Configuration
  @Profile("!inmem & !local")
  static class Postgres {
    @Bean
    IPresenceRepository presenceRepository(JdbcTemplate jdbc) {
      return new JdbcPresenceRepository(jdbc);
    }
  }
}
