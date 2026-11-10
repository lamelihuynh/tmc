package vn.tmc.gatestation.application.port.in;

import java.util.UUID;
import vn.tmc.gatestation.application.port.in.IGateAccess.ScanResult;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.platform.security.Principal;

/**
 * UC-06: a guard opens the gate for a verified employee. Reason is mandatory and audited (FR-20).
 */
public interface IGateManualAccess {

  record ManualOpenRequest(
      String gateId, UUID employeeUuid, Direction direction, String reason, String correlationId) {}

  ScanResult manualOpen(Principal guard, ManualOpenRequest request);
}
