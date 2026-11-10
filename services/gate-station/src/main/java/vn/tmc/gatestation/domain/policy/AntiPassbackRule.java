package vn.tmc.gatestation.domain.policy;

import java.util.Optional;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * FR-18: forbids two consecutive entries or two consecutive exits. Bidirectional gates infer
 * direction.
 */
public class AntiPassbackRule implements AccessPolicy {
  @Override
  public Optional<ReasonCode> evaluate(AccessContext ctx) {
    return ctx.last().filter(l -> l.direction() == ctx.direction()).isPresent()
        ? Optional.of(ReasonCode.ANTI_PASSBACK_VIOLATION)
        : Optional.empty();
  }
}
