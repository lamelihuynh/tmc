package vn.tmc.gatestation.application.port.in;

import java.util.UUID;

/** Used by Attendance reconcile to learn what the turnstile really did for an unknown attempt. */
public interface IGateObservation {

  enum Observation {
    OPENED,
    NOT_OPENED,
    UNDETERMINED
  }

  Observation observe(UUID attemptId, String gateId);
}
