package vn.tmc.attendance.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.attendance.application.port.in.AnomalyView;
import vn.tmc.attendance.application.port.in.AttemptView;
import vn.tmc.attendance.application.port.in.IAttendanceQuery;
import vn.tmc.attendance.application.port.in.LastPassView;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Rbac;
import vn.tmc.platform.security.Role;

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

  @Override
  public List<AnomalyView> anomalies(
      Principal caller, String factoryCode, Instant from, Instant to, int limit) {
    Rbac.requireRole(caller, Role.HR_STAFF);
    int boundedLimit = limit <= 0 ? 100 : Math.min(limit, 500);
    return attempts.findAnomalies(factoryCode, from, to, boundedLimit).stream()
        .map(AnomalyView::of)
        .toList();
  }
}
