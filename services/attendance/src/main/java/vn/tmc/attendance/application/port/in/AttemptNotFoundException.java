package vn.tmc.attendance.application.port.in;

import java.util.UUID;

public class AttemptNotFoundException extends RuntimeException {
  public AttemptNotFoundException(UUID attemptId) {
    super("attempt not found: " + attemptId);
  }
}
