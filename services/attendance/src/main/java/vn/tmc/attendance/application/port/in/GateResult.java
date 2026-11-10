package vn.tmc.attendance.application.port.in;

/** Normalized turnstile outcome (ITurnstileController contract, document.md section 12.2). */
public enum GateResult {
  OPENED,
  ERROR,
  UNKNOWN
}
