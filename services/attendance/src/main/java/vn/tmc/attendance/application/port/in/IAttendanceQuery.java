package vn.tmc.attendance.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.platform.security.Principal;

/** Read side (ISP: consumers that only read never see the write methods). */
public interface IAttendanceQuery {
  Optional<AttemptView> findAttempt(UUID attemptId);

  /** Latest counted pass strictly before {@code before}; empty when there is none. */
  Optional<LastPassView> lastPass(UUID employeeUuid, Instant before);

  /** Unresolved offline anomalies for HR review (UC-09, B-03). Requires HR_STAFF. */
  List<AnomalyView> anomalies(
      Principal caller, String factoryCode, Instant from, Instant to, int limit);
}
