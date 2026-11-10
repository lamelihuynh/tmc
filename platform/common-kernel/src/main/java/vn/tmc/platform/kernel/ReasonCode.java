package vn.tmc.platform.kernel;

/**
 * Standardized denial / outcome reasons exposed to kiosks and audit (document.md section 16.2,
 * ADR-11, ADR-12). Adding a code is additive; never repurpose an existing one.
 */
public enum ReasonCode {
  QR_EXPIRED,
  QR_ALREADY_USED,
  QR_REVOKED,
  INVALID_SIGNATURE,
  MALFORMED_TOKEN,
  DEVICE_UNKNOWN,
  DEVICE_REVOKED,
  CLOCK_SKEW_EXCEEDED,
  EMPLOYEE_INACTIVE,
  FACTORY_NOT_PERMITTED,
  GATE_DIRECTION_MISMATCH,
  ANTI_PASSBACK_VIOLATION,
  GATE_IN_MAINTENANCE,
  GATE_NOT_FOUND,
  TURNSTILE_TIMEOUT_RETRY_SCAN,
  GATE_HARDWARE_ERROR,
  SYSTEM_RETRY,
  IN_PROGRESS
}
