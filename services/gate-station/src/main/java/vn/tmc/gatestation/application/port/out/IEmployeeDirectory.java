package vn.tmc.gatestation.application.port.out;

import java.util.Optional;
import java.util.UUID;
import vn.tmc.gatestation.domain.EmployeeAccess;

/**
 * Driven port: employee eligibility at the edge (active or not, which factories). No personal data.
 * Seeded from configuration, then refreshed from User/QR's sync bundle ({@link
 * IEmployeeDirectoryCache}, SyncEmployeeDirectoryUseCase).
 */
public interface IEmployeeDirectory {
  Optional<EmployeeAccess> find(UUID employeeUuid);
}
