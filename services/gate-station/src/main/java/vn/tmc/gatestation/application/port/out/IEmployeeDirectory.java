package vn.tmc.gatestation.application.port.out;

import java.util.Optional;
import java.util.UUID;
import vn.tmc.gatestation.domain.EmployeeAccess;

/**
 * Driven port: employee eligibility at the edge (active or not, which factories). No personal data.
 * TODO Team C (C-02): fed by the sync bundle instead of configuration.
 */
public interface IEmployeeDirectory {
  Optional<EmployeeAccess> find(UUID employeeUuid);
}
