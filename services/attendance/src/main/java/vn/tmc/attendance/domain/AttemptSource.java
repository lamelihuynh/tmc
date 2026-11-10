package vn.tmc.attendance.domain;

/**
 * Where the attempt came from. Only ONLINE and OFFLINE count automatically; MANUAL waits for
 * verification.
 */
public enum AttemptSource {
  ONLINE,
  OFFLINE,
  MANUAL
}
