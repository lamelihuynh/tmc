package vn.tmc.gatestation.domain.policy;

import java.util.Optional;
import vn.tmc.platform.kernel.ReasonCode;

public class ActiveEmployeeRule implements AccessPolicy {
  @Override
  public Optional<ReasonCode> evaluate(AccessContext ctx) {
    // unknown employee is denied too: fail-secure (DP-04)
    return ctx.employee() == null || !ctx.employee().active()
        ? Optional.of(ReasonCode.EMPLOYEE_INACTIVE)
        : Optional.empty();
  }
}
