package vn.tmc.attendance.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;
import vn.tmc.attendance.adapter.out.http.HttpGateStatusQuery;
import vn.tmc.attendance.adapter.out.inmem.ConfigurableGateStatusQuery;
import vn.tmc.attendance.adapter.out.inmem.InMemoryAttemptRepository;
import vn.tmc.attendance.adapter.out.inmem.InMemoryAttendanceTransactionRepository;
import vn.tmc.attendance.adapter.out.inmem.InMemoryOutboxRepository;
import vn.tmc.attendance.adapter.out.inmem.InMemoryStore;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.application.port.out.IGateStatusQuery;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.application.port.out.ITransactionRunner;
import vn.tmc.platform.messaging.EventPublisher;
import vn.tmc.platform.messaging.InMemoryEventBroker;

/**
 * Profile "inmem": no database, no broker. Used by unit/slice tests and the zero-infrastructure
 * demo.
 */
@Configuration
@Profile({"inmem", "local"})
public class InMemoryProfileConfig {
  @Bean
  IAttemptRepository attempts() {
    return new InMemoryAttemptRepository();
  }

  @Bean
  IAttendanceTransactionRepository transactions() {
    return new InMemoryAttendanceTransactionRepository();
  }

  @Bean
  IOutboxRepository outbox() {
    return new InMemoryOutboxRepository();
  }

  @Bean
  ITransactionRunner transactionRunner() {
    return new InMemoryStore();
  }

  @Bean
  EventPublisher eventPublisher() {
    return new InMemoryEventBroker();
  }

  /**
   * "stub" for tests, "http" when Gate Station is running (local demo) so reconcile sees the real
   * gate.
   */
  @Bean
  IGateStatusQuery gateStatusQuery(
      @Value("${tmc.attendance.gate-status:stub}") String mode,
      @Value("${tmc.attendance.gate-station-url:http://localhost:8081}") String gateStationUrl) {
    return "http".equalsIgnoreCase(mode)
        ? new HttpGateStatusQuery(RestClient.builder().baseUrl(gateStationUrl).build())
        : new ConfigurableGateStatusQuery();
  }
}
