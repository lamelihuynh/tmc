package vn.tmc.attendance.application.port.in;

import java.time.Instant;
import java.util.UUID;
import vn.tmc.platform.security.Principal;

/** HR-only verification of a manual gate opening (B-07, FR-20); attemptId is the replay key. */
public interface IManualAttendanceVerification {
  VerificationResult verify(Principal caller, VerifyManualAttendanceCommand command);

  record VerifyManualAttendanceCommand(UUID attemptId, String reason, String correlationId) {}

  /** The original verification evidence is returned unchanged on every authorized retry. */
  record VerificationResult(
      UUID attemptId,
      UUID verifiedBy,
      String reason,
      Instant verifiedAt,
      long aggregateVersion,
      boolean idempotentReplay) {}
}
