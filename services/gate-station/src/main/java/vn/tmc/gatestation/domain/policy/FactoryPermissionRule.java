package vn.tmc.gatestation.domain.policy;

import java.util.Optional;
import vn.tmc.platform.kernel.ReasonCode;

/** A-06: an employee has a home factory but may be allowed into the other one. */
public class FactoryPermissionRule implements AccessPolicy {
  @Override
  public Optional<ReasonCode> evaluate(AccessContext ctx) {
    return ctx.employee() != null
            && ctx.employee().allowedFactories().contains(ctx.gate().factoryCode())
        ? Optional.empty()
        : Optional.of(ReasonCode.FACTORY_NOT_PERMITTED);
  }
}
