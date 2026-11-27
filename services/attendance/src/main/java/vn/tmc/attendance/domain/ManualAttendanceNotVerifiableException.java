package vn.tmc.attendance.domain;

/** A manual pass must have a confirmed opening and complete, unflagged attendance identity. */
public class ManualAttendanceNotVerifiableException extends IllegalStateException {
  public ManualAttendanceNotVerifiableException() {
    super("manual attendance cannot be verified in its current state");
  }
}
