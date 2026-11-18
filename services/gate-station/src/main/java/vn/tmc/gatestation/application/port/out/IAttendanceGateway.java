package vn.tmc.gatestation.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.domain.LastPass;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Driven port: the Attendance service on the same edge node (ADR-13 steps 4 and 6). Gate Station
 * never imports Attendance code; this interface is its own view of that service (contract:
 * docs/api/internal-attendance.openapi.yaml). Every call is idempotent per attemptId.
 *
 * <p>Implementations throw a RuntimeException when Attendance cannot be reached; the use case
 * decides what that means at each step.
 */
public interface IAttendanceGateway {

  /** What Gate Station needs to know about a stored attempt in order to answer a retry. */
  enum State {
    RESERVED,
    GATE_COMMANDED,
    COMMITTED,
    DENIED,
    GATE_FAILED,
    GATE_UNKNOWN,
    /** Any state Gate Station does not act on (for example VALIDATED or GATE_OPENED). */
    OTHER
  }

  /** Normalized turnstile outcome as Attendance records it. */
  enum GateResult {
    OPENED,
    ERROR,
    UNKNOWN
  }

  enum OfflineOutcome {
    ACCEPTED,
    DUPLICATE,
    ANOMALY
  }

  /**
   * employeeUuid and direction are null when Attendance did not send them (older version) or the
   * token could not be read.
   */
  record AttemptState(
      UUID attemptId, State state, ReasonCode reason, UUID employeeUuid, Direction direction) {
    public AttemptState(UUID attemptId, State state, ReasonCode reason) {
      this(attemptId, state, reason, null, null);
    }
  }

  /**
   * @param source "ONLINE" or "MANUAL"
   */
  record AttemptRecord(
      UUID attemptId,
      UUID employeeUuid,
      String gateId,
      String factoryCode,
      Direction direction,
      String jti,
      String source,
      Instant scannedAt,
      String correlationId) {}

  /** employeeUuid, direction and jti are null when the token could not even be read. */
  record DenialRecord(
      UUID attemptId,
      UUID employeeUuid,
      String gateId,
      String factoryCode,
      Direction direction,
      String jti,
      Instant scannedAt,
      ReasonCode reason,
      String correlationId) {}

  /**
   * @param localStatus "OFFLINE_OPENED" or "OFFLINE_DENIED", as written in the kiosk journal
   */
  record OfflineItem(
      UUID attemptId,
      long journalSeq,
      UUID employeeUuid,
      String gateId,
      String factoryCode,
      Direction direction,
      String jti,
      Instant scannedAt,
      String localStatus,
      ReasonCode deniedReason) {}

  record OfflineItemResult(UUID attemptId, OfflineOutcome outcome, String detail) {}

  Optional<AttemptState> find(UUID attemptId);

  /** Stores the attempt as RESERVED. */
  AttemptState recordAttempt(AttemptRecord record);

  AttemptState recordDenied(DenialRecord record);

  /** Write-ahead marker: must return before the open command is sent. */
  AttemptState markGateCommanded(UUID attemptId);

  AttemptState recordGateResult(UUID attemptId, GateResult result, String correlationId);

  List<OfflineItemResult> ingestOffline(String kioskId, List<OfflineItem> items);

  /**
   * The employee's latest counted pass strictly before {@code before}, as Attendance (the system of
   * record) knows it. Used to rebuild anti-passback state when the fast store lost it (A-03).
   * Contract: GET /internal/v1/employees/{uuid}/last-direction.
   */
  Optional<LastPass> lastDirection(UUID employeeUuid, Instant before);
}
