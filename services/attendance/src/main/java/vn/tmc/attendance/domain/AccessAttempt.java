package vn.tmc.attendance.domain;

import java.time.Instant;
import java.util.UUID;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * One scan attempt, identified by attempt_id which is also event_id and the idempotency key end to
 * end (ADR-05, ADR-11). The aggregate owns its legal transitions; use cases never set the status
 * directly.
 *
 * <p>No raw QR token is stored: only the token id (jti) needed for replay detection (ADR-09).
 */
public final class AccessAttempt {
  private final UUID attemptId;
  private final UUID employeeUuid;
  private final String gateId;
  private final String factoryCode;
  private final Direction direction;
  private final String jti;
  private final AttemptSource source;
  private final Instant scannedAt;

  private AttemptStatus status;
  private ReasonCode reasonCode;
  private Anomaly anomaly;
  private Instant updatedAt;
  private long version;

  private AccessAttempt(
      UUID attemptId,
      UUID employeeUuid,
      String gateId,
      String factoryCode,
      Direction direction,
      String jti,
      AttemptSource source,
      Instant scannedAt,
      AttemptStatus status,
      ReasonCode reasonCode,
      Anomaly anomaly,
      Instant updatedAt,
      long version) {
    this.attemptId = attemptId;
    this.employeeUuid = employeeUuid;
    this.gateId = gateId;
    this.factoryCode = factoryCode;
    this.direction = direction;
    this.jti = jti;
    this.source = source;
    this.scannedAt = scannedAt;
    this.status = status;
    this.reasonCode = reasonCode;
    this.anomaly = anomaly;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  /** New attempt that passed validation at the edge (VALIDATED). */
  public static AccessAttempt validated(
      UUID attemptId,
      UUID employeeUuid,
      String gateId,
      String factoryCode,
      Direction direction,
      String jti,
      AttemptSource source,
      Instant scannedAt,
      Instant now) {
    return new AccessAttempt(
        attemptId,
        employeeUuid,
        gateId,
        factoryCode,
        direction,
        jti,
        source,
        scannedAt,
        AttemptStatus.VALIDATED,
        null,
        null,
        now,
        1);
  }

  /** Rebuilds an attempt from storage without re-validating transitions. */
  public static AccessAttempt restore(
      UUID attemptId,
      UUID employeeUuid,
      String gateId,
      String factoryCode,
      Direction direction,
      String jti,
      AttemptSource source,
      Instant scannedAt,
      AttemptStatus status,
      ReasonCode reasonCode,
      Anomaly anomaly,
      Instant updatedAt,
      long version) {
    return new AccessAttempt(
        attemptId,
        employeeUuid,
        gateId,
        factoryCode,
        direction,
        jti,
        source,
        scannedAt,
        status,
        reasonCode,
        anomaly,
        updatedAt,
        version);
  }

  public void reserve(Instant now) {
    transitionTo(AttemptStatus.RESERVED, now);
  }

  /** Must be persisted BEFORE the open command leaves the edge (write-ahead, ADR-11 section 3). */
  public void commandGate(Instant now) {
    transitionTo(AttemptStatus.GATE_COMMANDED, now);
  }

  public void gateOpened(Instant now) {
    transitionTo(AttemptStatus.GATE_OPENED, now);
  }

  public void gateFailed(ReasonCode reason, Instant now) {
    transitionTo(AttemptStatus.GATE_FAILED, now);
    this.reasonCode = reason;
  }

  /** Timeout or crash after the command: never resend the open command, reconcile instead. */
  public void gateUnknown(Instant now) {
    transitionTo(AttemptStatus.GATE_UNKNOWN, now);
    this.reasonCode = ReasonCode.TURNSTILE_TIMEOUT_RETRY_SCAN;
  }

  public void commit(Instant now) {
    transitionTo(AttemptStatus.COMMITTED, now);
    this.reasonCode = null;
  }

  public void deny(ReasonCode reason, Instant now) {
    transitionTo(AttemptStatus.DENIED, now);
    this.reasonCode = reason;
  }

  /** Marks an offline record that must not count as attendance until HR verifies it. */
  public void flagAnomaly(Anomaly anomaly) {
    this.anomaly = anomaly;
  }

  private void transitionTo(AttemptStatus next, Instant now) {
    if (!status.allowedNext().contains(next)) {
      throw new IllegalAttemptTransitionException(status, next);
    }
    this.status = next;
    this.updatedAt = now;
    this.version++;
  }

  public UUID attemptId() {
    return attemptId;
  }

  public UUID employeeUuid() {
    return employeeUuid;
  }

  public String gateId() {
    return gateId;
  }

  public String factoryCode() {
    return factoryCode;
  }

  public Direction direction() {
    return direction;
  }

  public String jti() {
    return jti;
  }

  public AttemptSource source() {
    return source;
  }

  public Instant scannedAt() {
    return scannedAt;
  }

  public AttemptStatus status() {
    return status;
  }

  public ReasonCode reasonCode() {
    return reasonCode;
  }

  public Anomaly anomaly() {
    return anomaly;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public long version() {
    return version;
  }

  /**
   * True when this attempt is a counted pass: committed, no anomaly, not a manual open awaiting
   * verification.
   */
  public boolean countsAsAttendance() {
    return status == AttemptStatus.COMMITTED && anomaly == null && source != AttemptSource.MANUAL;
  }
}
