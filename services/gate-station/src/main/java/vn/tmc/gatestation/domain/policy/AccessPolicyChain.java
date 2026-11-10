package vn.tmc.gatestation.domain.policy;

import java.util.List;
import java.util.Optional;
import vn.tmc.platform.kernel.ReasonCode;

/** Runs the rules in order; the first denial wins. */
public class AccessPolicyChain {
  private final List<AccessPolicy> rules;

  public AccessPolicyChain(List<AccessPolicy> rules) {
    this.rules = List.copyOf(rules);
  }

  public static AccessPolicyChain standard() {
    return new AccessPolicyChain(
        List.of(new ActiveEmployeeRule(), new FactoryPermissionRule(), new AntiPassbackRule()));
  }

  public Optional<ReasonCode> evaluate(AccessContext context) {
    for (var rule : rules) {
      var denial = rule.evaluate(context);
      if (denial.isPresent()) {
        return denial;
      }
    }
    return Optional.empty();
  }
}
