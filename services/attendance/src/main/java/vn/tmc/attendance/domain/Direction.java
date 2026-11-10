package vn.tmc.attendance.domain;

/**
 * Direction of a pass through a gate. Bidirectional gates resolve to one of these before recording.
 */
public enum Direction {
  ENTRY,
  EXIT;

  public Direction opposite() {
    return this == ENTRY ? EXIT : ENTRY;
  }
}
