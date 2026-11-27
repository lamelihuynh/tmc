package vn.tmc.userqr.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import vn.tmc.platform.security.Principal;

/**
 * Driving port for HR updates to employee eligibility (C-02). Kept separate from {@link
 * IDeviceManagement} and {@link ISyncBundle} (ISP).
 */
public interface IEmployeeEligibility {

  enum Status {
    ACTIVE,
    INACTIVE
  }

  record EmployeeEligibilityView(
      UUID employeeUuid, Status status, List<String> factories, Instant updatedAt) {}

  /**
   * HR Staff updates an employee's status and permitted factories. Setting status to {@link
   * Status#INACTIVE} also revokes the employee's currently ACTIVE device, if any.
   */
  EmployeeEligibilityView update(
      Principal caller, UUID employeeUuid, Status status, Set<String> factories);
}
