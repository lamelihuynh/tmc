package vn.tmc.attendance.application.port.in;

/** Only an already-counted pass can be corrected; manual opens must be verified first. */
public class AttendanceNotCountedException extends RuntimeException {
  public AttendanceNotCountedException() {
    super("attempt has no counted attendance record");
  }
}
