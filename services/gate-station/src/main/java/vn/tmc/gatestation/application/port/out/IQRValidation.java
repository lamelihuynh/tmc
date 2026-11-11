package vn.tmc.gatestation.application.port.out;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Driven port: the QR Verifier of User/QR (ADR-13 step 1, contract: docs/api/user-qr.openapi.yaml
 * POST /internal/v1/qr/verify). Verification is read-only; one-time use is enforced afterwards by
 * {@link ITokenReservation}.
 */
public interface IQRValidation {

  /** Field names match the JSON returned by User/QR. Never carries the raw token. */
  record Verdict(
      boolean valid,
      ReasonCode reason,
      UUID employeeUuid,
      String deviceId,
      String jti,
      Instant expiresAt,
      Set<String> factories) {

    public static Verdict denied(ReasonCode reason) {
      return new Verdict(false, reason, null, null, null, null, Set.of());
    }

    public static Verdict ok(UUID employeeUuid, String jti, Instant expiresAt) {
      return new Verdict(true, null, employeeUuid, null, jti, expiresAt, Set.of());
    }
  }

  Verdict verify(String compactToken);
}
