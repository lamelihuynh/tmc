package vn.tmc.attendance.application.port.in;

/** A correction key already belongs to a different request, or its revision was taken. */
public class CorrectionConflictException extends RuntimeException {
  public CorrectionConflictException() {
    super("correction conflicts with an existing request");
  }
}
