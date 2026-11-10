package vn.tmc.reporting.domain;

import java.util.Optional;

/**
 * Behavioral fraud rule over confirmed passes (ADR-12 R4). TODO Team D: implement
 * ImpossibleTravelRule (the same employee in two factories closer in time than the configured
 * travel time) and RepeatedDenialRule (many expired or invalid tokens from one subject or kiosk).
 * Each rule only returns a finding; raising the alert and flagging attendance for HR review is the
 * use case's job.
 */
public interface FraudRule {
  record Finding(String rule, String detail) {}

  Optional<Finding> evaluate(Presence previous, Presence current);
}
