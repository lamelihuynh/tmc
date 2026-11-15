package vn.tmc.attendance.application.port.in;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Read side (ISP: consumers that only read never see the write methods). */
public interface IAttendanceQuery {
  Optional<AttemptView> findAttempt(UUID attemptId);

  /** Latest counted pass strictly before {@code before}; empty when there is none. */
  Optional<LastPassView> lastPass(UUID employeeUuid, Instant before);
}
