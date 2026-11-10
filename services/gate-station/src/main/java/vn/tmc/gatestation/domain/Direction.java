package vn.tmc.gatestation.domain;

public enum Direction {
  ENTRY,
  EXIT;

  public Direction opposite() {
    return this == ENTRY ? EXIT : ENTRY;
  }
}
