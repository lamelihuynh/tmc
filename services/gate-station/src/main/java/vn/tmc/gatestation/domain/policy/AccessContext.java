package vn.tmc.gatestation.domain.policy;

import java.time.Instant;
import java.util.Optional;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.domain.EmployeeAccess;
import vn.tmc.gatestation.domain.GateConfig;
import vn.tmc.gatestation.domain.LastPass;

/** Everything a rule may look at. Rules never call out to infrastructure (DP-01). */
public record AccessContext(
    EmployeeAccess employee,
    GateConfig gate,
    Direction direction,
    Optional<LastPass> last,
    Instant scannedAt) {}
