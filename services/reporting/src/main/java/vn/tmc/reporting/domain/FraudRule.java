package vn.tmc.reporting.domain;

import java.util.Optional;

/**
 * Behavioral fraud rule over projected passes (ADR-12 R4). A rule only returns a finding; storing
 * the alert and flagging the pass for HR review is the use case's job, and nothing is deleted.
 * Implemented: {@link ImpossibleTravelRule}. Not implemented: a repeated-denial rule (many expired
 * or invalid tokens from one subject or kiosk), which needs denial events this projection does not
 * consume.
 */
public interface FraudRule {
  record Finding(String rule, String detail) {}

  /**
   * @param previous the employee's latest pass in another factory
   * @param current the pass just projected
   */
  Optional<Finding> evaluate(Presence previous, Presence current);
}
