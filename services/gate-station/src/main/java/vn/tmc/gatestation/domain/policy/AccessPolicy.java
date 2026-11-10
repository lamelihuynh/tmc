package vn.tmc.gatestation.domain.policy;

import java.util.Optional;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * One access rule (Strategy). Open for extension, closed for modification (OCP, AC-09): adding a
 * rule such as a shift window means adding a class and registering it, never editing
 * RecordScanUseCase.
 */
@FunctionalInterface
public interface AccessPolicy {

  /**
   * @return the denial reason, or empty when this rule has no objection
   */
  Optional<ReasonCode> evaluate(AccessContext context);
}
