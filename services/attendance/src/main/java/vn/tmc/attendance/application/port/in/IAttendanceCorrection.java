package vn.tmc.attendance.application.port.in;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import vn.tmc.attendance.domain.AttendanceCorrection;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.security.Principal;

/** HR-only, append-only correction of a counted pass (FR-26, B-06). */
public interface IAttendanceCorrection {
  CorrectionView correct(Principal caller, CorrectAttendanceCommand command);

  /** The caller's authenticated subject supplies correctedBy; clients cannot choose the actor. */
  record CorrectAttendanceCommand(
      UUID correctionId,
      UUID originalAttemptId,
      Direction direction,
      Instant scannedAt,
      String reason,
      String correlationId) {
    private static final Pattern REASON_CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");

    public CorrectAttendanceCommand {
      Objects.requireNonNull(correctionId, "correctionId");
      Objects.requireNonNull(originalAttemptId, "originalAttemptId");
      Objects.requireNonNull(direction, "direction");
      Objects.requireNonNull(scannedAt, "scannedAt");
      if (reason == null || !REASON_CODE.matcher(reason).matches()) {
        throw new IllegalArgumentException("reason must be an UPPER_SNAKE_CASE code");
      }
      // PostgreSQL TIMESTAMPTZ stores microseconds. Canonicalize before storing or comparing a
      // retry so a sub-microsecond timestamp cannot turn an identical request into a conflict.
      scannedAt = scannedAt.truncatedTo(ChronoUnit.MICROS);
    }
  }

  record CorrectionView(
      AttendanceCorrection correction, boolean idempotentReplay, long aggregateVersion) {}
}
