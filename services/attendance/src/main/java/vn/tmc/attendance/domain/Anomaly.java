package vn.tmc.attendance.domain;

/**
 * Flags that keep an attempt out of automatic attendance until HR resolves it (UC-09, ADR-11
 * section 5).
 */
public enum Anomaly {
  TOKEN_REUSED_OFFLINE,
  ANTI_PASSBACK_OFFLINE
}
