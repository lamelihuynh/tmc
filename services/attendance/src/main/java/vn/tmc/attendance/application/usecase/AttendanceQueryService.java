package vn.tmc.attendance.application.usecase;

import java.util.Optional;
import java.util.UUID;
import vn.tmc.attendance.application.port.in.AttemptView;
import vn.tmc.attendance.application.port.in.IAttendanceQuery;
import vn.tmc.attendance.application.port.out.IAttemptRepository;

public class AttendanceQueryService implements IAttendanceQuery {
  private final IAttemptRepository attempts;

  public AttendanceQueryService(IAttemptRepository attempts) {
    this.attempts = attempts;
  }

  @Override
  public Optional<AttemptView> findAttempt(UUID attemptId) {
    return attempts.findById(attemptId).map(a -> AttemptView.of(a, false));
  }
}
