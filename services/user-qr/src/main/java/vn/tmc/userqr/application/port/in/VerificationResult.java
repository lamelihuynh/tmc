package vn.tmc.userqr.application.port.in;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import vn.tmc.platform.kernel.ReasonCode;

/** Outcome of IQRValidation. Never carries the raw token. */
public record VerificationResult(
    boolean valid,
    ReasonCode reason,
    UUID employeeUuid,
    String deviceId,
    String jti,
    Instant expiresAt,
    Set<String> factories) {

  public static VerificationResult denied(ReasonCode reason) {
    return new VerificationResult(false, reason, null, null, null, null, Set.of());
  }

  public static VerificationResult ok(
      UUID employee, String deviceId, String jti, Instant expiresAt, Set<String> factories) {
    return new VerificationResult(true, null, employee, deviceId, jti, expiresAt, factories);
  }
}
