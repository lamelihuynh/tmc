package vn.tmc.gatestation.domain;

import java.util.Set;
import java.util.UUID;

/**
 * What Gate Station needs to know about an employee: eligibility only, no personal data (ADR-09).
 */
public record EmployeeAccess(UUID employeeUuid, boolean active, Set<String> allowedFactories) {
  public EmployeeAccess {
    allowedFactories = allowedFactories == null ? Set.of() : Set.copyOf(allowedFactories);
  }
}
