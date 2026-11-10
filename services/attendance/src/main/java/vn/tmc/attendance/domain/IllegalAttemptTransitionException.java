package vn.tmc.attendance.domain;

public class IllegalAttemptTransitionException extends RuntimeException {
  public IllegalAttemptTransitionException(AttemptStatus from, AttemptStatus to) {
    super("illegal attempt transition " + from + " -> " + to);
  }
}
