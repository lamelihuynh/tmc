package vn.tmc.gatestation.bootstrap;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import vn.tmc.gatestation.domain.EmployeeAccess;
import vn.tmc.gatestation.domain.GateConfig;
import vn.tmc.gatestation.domain.GateMode;

/**
 * Seed data and endpoints (DP-10 Configuration over Code). Gates and employee eligibility are
 * seeded from configuration for the scaffold. TODO Team A: Gate Store in PostgreSQL. TODO Team C:
 * employee eligibility arrives through the sync bundle.
 */
@ConfigurationProperties(prefix = "tmc.gate")
public record GateProperties(
    List<Gate> gates, List<Employee> employees, Turnstile turnstile, Urls urls, Scan scan) {

  public record Gate(
      String gateId,
      String factoryCode,
      String hardwareGateId,
      GateMode mode,
      boolean maintenance) {
    GateConfig toConfig() {
      return new GateConfig(gateId, factoryCode, hardwareGateId, mode, maintenance);
    }
  }

  public record Employee(UUID employeeUuid, boolean active, Set<String> factories) {
    EmployeeAccess toAccess() {
      return new EmployeeAccess(employeeUuid, active, factories);
    }
  }

  /** mode is "http" (real or turnstile-simulator service) or "simulated" (in-process). */
  public record Turnstile(
      String mode, String baseUrl, String token, long connectTimeoutMs, long readTimeoutMs) {}

  public record Urls(String attendance, String userQr) {}

  public record Scan(long reservationTtlSeconds, long pendingTtlSeconds, long tokenBlockSeconds) {}

  public GateProperties {
    gates = gates == null ? List.of() : gates;
    employees = employees == null ? List.of() : employees;
  }
}
