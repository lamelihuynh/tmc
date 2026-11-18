package vn.tmc.gatestation.application.port.out;

import java.util.UUID;
import vn.tmc.gatestation.domain.Direction;

/**
 * Driven port: the existing Turnstile Control System (document.md 16.3). The adapter is an
 * anti-corruption layer: vendor status codes and error names never reach the use cases, only these
 * three outcomes.
 */
public interface ITurnstileControlPort {

  enum Trigger {
    QR_SCAN,
    MANUAL_GUARD_OVERRIDE
  }

  /**
   * @param commandKey the attempt id; the adapter derives the vendor idempotency key from it, and
   *     the same key is used later to ask what the command did
   * @param operatorId the guard for a manual override, null for a QR scan
   */
  record OpenGateRequest(
      String hardwareGateId,
      Direction direction,
      String commandKey,
      Trigger trigger,
      UUID operatorId,
      String correlationId) {}

  enum CommandResult {
    /** The controller confirmed the gate unlocked. */
    OPENED,
    /**
     * The controller definitely did not open (jam, offline, refused). Safe to release the token.
     */
    ERROR,
    /** No usable answer. The gate MAY have opened. Never answer this by sending another open. */
    TIMEOUT
  }

  enum CommandObservation {
    OPENED,
    NOT_OPENED,
    UNDETERMINED
  }

  CommandResult openGate(OpenGateRequest request);

  /** Read-only: what did the command with this key do? Used to resolve a TIMEOUT (ADR-11 P3). */
  CommandObservation queryCommand(String hardwareGateId, String commandKey);
}
