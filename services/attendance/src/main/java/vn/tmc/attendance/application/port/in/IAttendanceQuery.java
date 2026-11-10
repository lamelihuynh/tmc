package vn.tmc.attendance.application.port.in;

import java.util.Optional;
import java.util.UUID;

/** Read side (ISP: consumers that only read never see the write methods). */
public interface IAttendanceQuery {
  Optional<AttemptView> findAttempt(UUID attemptId);
}
