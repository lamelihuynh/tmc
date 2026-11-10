package vn.tmc.userqr.application.port.in;

/**
 * Read-only QR verification (ADR-13 step 1). Replay is NOT decided here: one-time use is enforced
 * by the token reservation owned by Gate Station, so a failed gate command can release the token
 * (document.md 16.3.2).
 */
public interface IQRValidation {
  VerificationResult verify(String compactToken);
}
