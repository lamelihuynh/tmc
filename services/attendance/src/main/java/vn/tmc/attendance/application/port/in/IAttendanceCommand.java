package vn.tmc.attendance.application.port.in;

import java.time.Instant;
import java.util.UUID;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Write side used by Gate Station (ADR-13 steps 4 and 6). Every method is idempotent per attemptId
 * (ADR-05): a replay returns the stored result and creates nothing new.
 */
public interface IAttendanceCommand {

  /** Persists the attempt as RESERVED (token and anti-passback slot are held by the caller). */
  AttemptView recordAttempt(RecordAttemptCommand command);

  /** Persists a denial so a retried request returns the same answer and audit has a trace. */
  AttemptView recordDenied(RecordDeniedCommand command);

  /** Write-ahead marker: must succeed BEFORE the open command is sent to the turnstile. */
  AttemptView markGateCommanded(UUID attemptId);

  /**
   * Applies the normalized turnstile outcome; OPENED commits the attendance and its outbox events.
   */
  AttemptView recordGateResult(UUID attemptId, GateResult result, String correlationId);

  record RecordAttemptCommand(
      UUID attemptId,
      UUID employeeUuid,
      String gateId,
      String factoryCode,
      Direction direction,
      String jti,
      AttemptSource source,
      Instant scannedAt,
      String correlationId) {}

  /** employeeUuid, direction and jti may be null when the token could not even be parsed. */
  record RecordDeniedCommand(
      UUID attemptId,
      UUID employeeUuid,
      String gateId,
      String factoryCode,
      Direction direction,
      String jti,
      Instant scannedAt,
      ReasonCode reasonCode,
      String correlationId) {}
}
