package vn.tmc.attendance.application.port.in;

import java.time.Instant;
import java.util.UUID;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.Anomaly;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.attendance.domain.Direction;

/** Read model for unresolved offline anomalies reviewed by HR (UC-09, B-03). */
public record AnomalyView(
    UUID attemptId,
    UUID employeeUuid,
    String factoryCode,
    String gateId,
    Direction direction,
    Anomaly anomaly,
    AttemptStatus status,
    Instant scannedAt,
    Instant updatedAt) {

  public static AnomalyView of(AccessAttempt attempt) {
    return new AnomalyView(
        attempt.attemptId(),
        attempt.employeeUuid(),
        attempt.factoryCode(),
        attempt.gateId(),
        attempt.direction(),
        attempt.anomaly(),
        attempt.status(),
        attempt.scannedAt(),
        attempt.updatedAt());
  }
}
