package vn.tmc.attendance.application.usecase;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.attendance.application.port.in.AttemptView;
import vn.tmc.attendance.application.port.in.IAttendanceQuery;
import vn.tmc.attendance.application.port.in.LastPassView;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;

public class AttendanceQueryService implements IAttendanceQuery {
  private final IAttemptRepository attempts;
  private final IAttendanceTransactionRepository transactions;

  public AttendanceQueryService(
      IAttemptRepository attempts, IAttendanceTransactionRepository transactions) {
    this.attempts = attempts;
    this.transactions = transactions;
  }

  @Override
  public Optional<AttemptView> findAttempt(UUID attemptId) {
    return attempts.findById(attemptId).map(a -> AttemptView.of(a, false));
  }

  @Override
  public Optional<LastPassView> lastPass(UUID employeeUuid, Instant before) {
    return transactions.latestBefore(employeeUuid, before).map(LastPassView::of);
  }
}
