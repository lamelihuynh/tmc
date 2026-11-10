package vn.tmc.attendance.application.port.in;

import java.util.UUID;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.Anomaly;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Read model returned to callers. {@code duplicate} is true when the call was an idempotent replay.
 */
public record AttemptView(
    UUID attemptId,
    AttemptStatus status,
    ReasonCode reasonCode,
    Anomaly anomaly,
    boolean duplicate) {

  public static AttemptView of(AccessAttempt attempt, boolean duplicate) {
    return new AttemptView(
        attempt.attemptId(), attempt.status(), attempt.reasonCode(), attempt.anomaly(), duplicate);
  }
}
