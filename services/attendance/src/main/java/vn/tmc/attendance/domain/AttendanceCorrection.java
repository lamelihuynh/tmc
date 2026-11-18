package vn.tmc.attendance.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * One HR correction of a counted pass (story B-06, docs/api/attendance-events.asyncapi.yaml
 * AttendanceCorrected). It never edits the original row: it is a new row that points at it (table
 * attendance_corrections, migration V2), so the history stays auditable.
 *
 * <p>The values are the FULL corrected values (not a delta). {@code reason} is a code, never free
 * text, so no personal data can slip in (ADR-09). {@code correctedBy} is the internal UUID of the
 * HR user.
 */
public record AttendanceCorrection(
    UUID correctionId,
    UUID originalAttemptId,
    int revision,
    UUID employeeUuid,
    Direction direction,
    Instant scannedAt,
    Direction previousDirection,
    Instant previousScannedAt,
    String reason,
    UUID correctedBy,
    AttemptSource source,
    Instant correctedAt) {

  private static final Pattern REASON_CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");

  public AttendanceCorrection {
    Objects.requireNonNull(correctionId, "correctionId");
    Objects.requireNonNull(originalAttemptId, "originalAttemptId");
    Objects.requireNonNull(employeeUuid, "employeeUuid");
    Objects.requireNonNull(direction, "direction");
    Objects.requireNonNull(scannedAt, "scannedAt");
    Objects.requireNonNull(previousDirection, "previousDirection");
    Objects.requireNonNull(previousScannedAt, "previousScannedAt");
    Objects.requireNonNull(correctedBy, "correctedBy");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(correctedAt, "correctedAt");
    if (revision < 1) {
      throw new IllegalArgumentException("revision starts at 1");
    }
    if (reason == null || !REASON_CODE.matcher(reason).matches()) {
      throw new IllegalArgumentException("reason must be an UPPER_SNAKE_CASE code, not free text");
    }
  }

  /**
   * Correction number {@code revision} of {@code original}. {@code previous*} are taken from the
   * values in force before this correction: the original for revision 1, the last correction
   * otherwise (pass them through {@code current}).
   */
  public static AttendanceCorrection of(
      UUID correctionId,
      AttendanceTransaction original,
      int revision,
      Direction currentDirection,
      Instant currentScannedAt,
      Direction direction,
      Instant scannedAt,
      String reason,
      UUID correctedBy,
      Instant now) {
    return new AttendanceCorrection(
        correctionId,
        original.attemptId(),
        revision,
        original.employeeUuid(),
        direction,
        scannedAt,
        currentDirection,
        currentScannedAt,
        reason,
        correctedBy,
        original.source(),
        now);
  }

  /** First correction of {@code original}. */
  public static AttendanceCorrection first(
      UUID correctionId,
      AttendanceTransaction original,
      Direction direction,
      Instant scannedAt,
      String reason,
      UUID correctedBy,
      Instant now) {
    return of(
        correctionId,
        original,
        1,
        original.direction(),
        original.occurredAt(),
        direction,
        scannedAt,
        reason,
        correctedBy,
        now);
  }
}
