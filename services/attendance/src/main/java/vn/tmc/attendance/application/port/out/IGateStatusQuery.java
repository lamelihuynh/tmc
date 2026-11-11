package vn.tmc.attendance.application.port.out;

import vn.tmc.attendance.domain.AccessAttempt;

/**
 * Driven port: asks Gate Station what the turnstile really did for an attempt whose outcome is
 * unknown (ADR-11 P3). It only observes; it can never cause a new open command.
 */
public interface IGateStatusQuery {

  enum GateObservation {
    /** The command for this attempt unlocked the gate: the pass counts. */
    OPENED,
    /** The gate never opened for this attempt. */
    NOT_OPENED,
    /** Cannot tell (gate unreachable, no record): stays unknown for the guard. */
    UNDETERMINED
  }

  GateObservation observe(AccessAttempt attempt);
}
