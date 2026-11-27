package vn.tmc.reporting.bootstrap;

import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.ProcessedMessageStore;
import vn.tmc.reporting.adapter.out.inmem.InMemoryFraudAlertStore;
import vn.tmc.reporting.adapter.out.inmem.InMemoryPresenceRepository;
import vn.tmc.reporting.adapter.out.persistence.JdbcFraudAlertStore;
import vn.tmc.reporting.adapter.out.persistence.JdbcPresenceRepository;
import vn.tmc.reporting.application.port.in.IReporting;
import vn.tmc.reporting.application.port.out.IFraudAlertStore;
import vn.tmc.reporting.application.port.out.IPresenceRepository;
import vn.tmc.reporting.application.usecase.ProjectAttendanceEventUseCase;
import vn.tmc.reporting.application.usecase.ReportingQueryService;
import vn.tmc.reporting.domain.FraudRule;
import vn.tmc.reporting.domain.ImpossibleTravelRule;

@Configuration
@Import(DedupStoreConfig.class)
public class ReportingConfig {
  /** ADR-12 R4; the travel window is {@code tmc.reporting.fraud.travel-window} (default 60 min). */
  @Bean
  List<FraudRule> fraudRules(
      @Value("${tmc.reporting.fraud.travel-window:PT60M}") Duration travelWindow) {
    return List.of(new ImpossibleTravelRule(travelWindow));
  }

  @Bean
  ProjectAttendanceEventUseCase projectAttendanceEvent(
      IPresenceRepository presences, List<FraudRule> fraudRules, IFraudAlertStore alerts) {
    return new ProjectAttendanceEventUseCase(presences, fraudRules, alerts);
  }

  @Bean
  IdempotentConsumer reportingConsumer(
      ProcessedMessageStore store,
      IdempotentConsumer.TransactionRunner tx,
      ProjectAttendanceEventUseCase projection) {
    return new IdempotentConsumer("reporting", store, projection::handle, tx);
  }

  @Bean
  IReporting reporting(IPresenceRepository presences, IFraudAlertStore alerts) {
    return new ReportingQueryService(presences, alerts);
  }

  /** No database: the read model lives in process memory and is lost on restart. */
  @Configuration
  @Profile({"inmem", "local"})
  static class InMemory {
    @Bean
    IPresenceRepository presenceRepository() {
      return new InMemoryPresenceRepository();
    }

    @Bean
    IFraudAlertStore fraudAlertStore() {
      return new InMemoryFraudAlertStore();
    }
  }

  /**
   * PostgreSQL (D-04, D-05): tables attendance_projection and fraud_alerts, written in the dedup
   * transaction set up by DedupStoreConfig, so presence and alerts survive a restart and a failed
   * handler leaves no partial state.
   */
  @Configuration
  @Profile("!inmem & !local")
  static class Postgres {
    @Bean
    IPresenceRepository presenceRepository(JdbcTemplate jdbc) {
      return new JdbcPresenceRepository(jdbc);
    }

    @Bean
    IFraudAlertStore fraudAlertStore(JdbcTemplate jdbc) {
      return new JdbcFraudAlertStore(jdbc);
    }
  }
}
