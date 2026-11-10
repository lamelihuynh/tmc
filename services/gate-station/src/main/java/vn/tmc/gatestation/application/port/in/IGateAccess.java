package vn.tmc.gatestation.application.port.in;

import java.time.Instant;
import java.util.UUID;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Kiosk-facing scan use case (ADR-13). attemptId is the Idempotency-Key: a retry never opens twice.
 */
public interface IGateAccess {

  record ScanRequest(
      UUID attemptId,
      String gateId,
      String qrToken,
      Instant scannedAt,
      String kioskId,
      String correlationId) {}

  /**
   * ALLOWED: gate opened and recorded. DENIED: rejected, gate stayed closed. PENDING: command sent
   * but outcome unknown, do not rescan the same token, reconcile decides. IN_PROGRESS: a previous
   * request for this attemptId is still running. UNCONFIRMED: gate opened but the record could not
   * be saved yet; recovery completes it.
   */
  enum Outcome {
    ALLOWED,
    DENIED,
    PENDING,
    IN_PROGRESS,
    UNCONFIRMED
  }

  record ScanResult(
      UUID attemptId, Outcome outcome, ReasonCode reason, Direction direction, boolean duplicate) {}

  ScanResult scan(ScanRequest request);
}
