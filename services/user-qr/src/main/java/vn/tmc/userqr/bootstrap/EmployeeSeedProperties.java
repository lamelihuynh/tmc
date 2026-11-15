package vn.tmc.userqr.bootstrap;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Employee eligibility seed ({@code tmc.qr.employees} in application.yml). Must equal
 * docs/api/fixtures/employee-seed.json (EmployeeSeedContractTest). TODO Team C (C-02): replaced by
 * the HR feed.
 */
@ConfigurationProperties(prefix = "tmc.qr")
public record EmployeeSeedProperties(List<Employee> employees) {
  public record Employee(UUID employeeUuid, boolean active, Set<String> factories) {}

  public EmployeeSeedProperties {
    employees = employees == null ? List.of() : employees;
  }
}
