package vn.tmc.gatestation.application.usecase;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.tmc.gatestation.application.port.in.IGateAccess;
import vn.tmc.gatestation.application.port.out.IAntiPassbackStore;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.AttemptRecord;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.AttemptState;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.DenialRecord;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.GateResult;
import vn.tmc.gatestation.application.port.out.IEmployeeDirectory;
import vn.tmc.gatestation.application.port.out.IGateConfigRepository;
import vn.tmc.gatestation.application.port.out.IQRValidation;
import vn.tmc.gatestation.application.port.out.ITokenReservation;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.OpenGateRequest;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.Trigger;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.domain.DirectionResolver;
import vn.tmc.gatestation.domain.GateConfig;
import vn.tmc.gatestation.domain.policy.AccessContext;
import vn.tmc.gatestation.domain.policy.AccessPolicyChain;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Orchestrates one QR scan at the edge, in the exact order of ADR-13:
 *
 * <ol>
 *   <li>verify the token, 2. evaluate access rules, 3. reserve token and anti-passback slot,
 *   <li>record the attempt (RESERVED) and write-ahead GATE_COMMANDED, 5. open the gate,
 *   <li>record the normalized result, 7. answer the kiosk.
 * </ol>
 *
 * Safety rules that this class enforces (ADR-11): a retry with the same attemptId never reaches the
 * turnstile again; a timeout is recorded as UNKNOWN and is never answered by another open command;
 * every early exit releases whatever was reserved.
 */
public class RecordScanUseCase implements IGateAccess {
  private static final Logger log = LoggerFactory.getLogger(RecordScanUseCase.class);

  private final IGateConfigRepository gates;
  private final IQRValidation qr;
  private final IEmployeeDirectory employees;
  private final AccessPolicyChain policies;
  private final ITokenReservation tokens;
  private final IAntiPassbackStore antiPassback;
  private final IAttendanceGateway attendance;
  private final ITurnstileControlPort turnstile;
  private final ScanSettings settings;
  private final IClock clock;

  public RecordScanUseCase(
      IGateConfigRepository gates,
      IQRValidation qr,
      IEmployeeDirectory employees,
      AccessPolicyChain policies,
      ITokenReservation tokens,
      IAntiPassbackStore antiPassback,
      IAttendanceGateway attendance,
      ITurnstileControlPort turnstile,
      ScanSettings settings,
      IClock clock) {
    this.gates = gates;
    this.qr = qr;
    this.employees = employees;
    this.policies = policies;
    this.tokens = tokens;
    this.antiPassback = antiPassback;
    this.attendance = attendance;
    this.turnstile = turnstile;
    this.settings = settings;
    this.clock = clock;
  }

  @Override
  public ScanResult scan(ScanRequest r) {
    // P1 / P5: a retry gets the stored answer and never touches the gate again.
    Optional<AttemptState> existing = attendance.find(r.attemptId());
    if (existing.isPresent()) {
      return replay(existing.get());
    }

    GateConfig gate = gates.find(r.gateId()).orElse(null);
    if (gate == null) {
      return deny(r, "UNKNOWN", null, null, null, ReasonCode.GATE_NOT_FOUND);
    }
    if (!gate.acceptsScans()) {
      return deny(r, gate.factoryCode(), null, null, null, ReasonCode.GATE_IN_MAINTENANCE);
    }

    // step 1
    var verdict = qr.verify(r.qrToken());
    if (!verdict.valid()) {
      return deny(r, gate.factoryCode(), null, null, null, verdict.reason());
    }
    UUID employeeUuid = verdict.employeeUuid();

    // step 2
    var last = antiPassback.last(employeeUuid);
    Direction direction = DirectionResolver.resolve(gate, last);
    var context =
        new AccessContext(
            employees.find(employeeUuid).orElse(null), gate, direction, last, r.scannedAt());
    var denial = policies.evaluate(context);
    if (denial.isPresent()) {
      return deny(r, gate.factoryCode(), employeeUuid, direction, verdict.jti(), denial.get());
    }

    // step 3: atomic reservations
    if (!tokens.reserve(verdict.jti(), settings.reservationTtl())) {
      return deny(
          r,
          gate.factoryCode(),
          employeeUuid,
          direction,
          verdict.jti(),
          ReasonCode.QR_ALREADY_USED);
    }
    if (!antiPassback.tryReserve(employeeUuid, direction, r.scannedAt(), settings.pendingTtl())) {
      tokens.release(verdict.jti());
      return deny(
          r,
          gate.factoryCode(),
          employeeUuid,
          direction,
          verdict.jti(),
          ReasonCode.ANTI_PASSBACK_VIOLATION);
    }

    // step 4: durable before anything physical happens
    try {
      attendance.recordAttempt(
          new AttemptRecord(
              r.attemptId(),
              employeeUuid,
              gate.gateId(),
              gate.factoryCode(),
              direction,
              verdict.jti(),
              "ONLINE",
              r.scannedAt(),
              r.correlationId()));
      attendance.markGateCommanded(r.attemptId());
    } catch (RuntimeException e) {
      // Nothing was sent to the gate. Release so the person can rescan; Attendance recovery frees
      // the row.
      tokens.release(verdict.jti());
      antiPassback.release(employeeUuid);
      throw e;
    }

    // step 5
    var result =
        turnstile.openGate(
            new OpenGateRequest(
                gate.hardwareGateId(),
                direction,
                r.attemptId().toString(),
                Trigger.QR_SCAN,
                null,
                r.correlationId()));

    // steps 6 and 7
    return switch (result) {
      case OPENED -> onOpened(r, verdict.jti(), verdict.expiresAt(), employeeUuid, direction);
      case ERROR -> onError(r, verdict.jti(), employeeUuid, direction);
      case TIMEOUT -> onTimeout(r, verdict.jti(), verdict.expiresAt(), employeeUuid, direction);
    };
  }

  private ScanResult onOpened(
      ScanRequest r, String jti, Instant expiresAt, UUID employee, Direction direction) {
    tokens.markUsed(jti, expiresAt.plus(settings.tokenBlockAfterUse()));
    antiPassback.confirm(employee);
    try {
      attendance.recordGateResult(r.attemptId(), GateResult.OPENED, r.correlationId());
    } catch (RuntimeException e) {
      // The person already passed. Attendance recovery will move the row through UNKNOWN to
      // COMMITTED.
      log.warn("gate opened but result not recorded yet attempt={}", r.attemptId(), e);
      return new ScanResult(r.attemptId(), Outcome.UNCONFIRMED, null, direction, false);
    }
    return new ScanResult(r.attemptId(), Outcome.ALLOWED, null, direction, false);
  }

  private ScanResult onError(ScanRequest r, String jti, UUID employee, Direction direction) {
    safeRecordResult(r, GateResult.ERROR);
    tokens.release(jti);
    antiPassback.release(employee);
    return new ScanResult(
        r.attemptId(), Outcome.DENIED, ReasonCode.GATE_HARDWARE_ERROR, direction, false);
  }

  private ScanResult onTimeout(
      ScanRequest r, String jti, Instant expiresAt, UUID employee, Direction direction) {
    safeRecordResult(r, GateResult.UNKNOWN);
    // Unknown means the gate may have opened: keep the token blocked and the slot pending until
    // reconcile.
    tokens.markUsed(jti, expiresAt.plus(settings.tokenBlockAfterUse()));
    antiPassback.keepPending(employee, settings.pendingTtl());
    return new ScanResult(
        r.attemptId(), Outcome.PENDING, ReasonCode.TURNSTILE_TIMEOUT_RETRY_SCAN, direction, false);
  }

  private void safeRecordResult(ScanRequest r, GateResult result) {
    try {
      attendance.recordGateResult(r.attemptId(), result, r.correlationId());
    } catch (RuntimeException e) {
      // Row stays GATE_COMMANDED; the recovery job turns it into UNKNOWN and reconcile resolves it.
      log.warn("could not record gate result {} attempt={}", result, r.attemptId(), e);
    }
  }

  private ScanResult deny(
      ScanRequest r,
      String factory,
      UUID employee,
      Direction direction,
      String jti,
      ReasonCode reason) {
    try {
      attendance.recordDenied(
          new DenialRecord(
              r.attemptId(),
              employee,
              r.gateId(),
              factory,
              direction,
              jti,
              r.scannedAt(),
              reason,
              r.correlationId()));
    } catch (RuntimeException e) {
      // A denial opens nothing, so failing to log it must not turn into an approval.
      log.warn("could not record denial {} attempt={}", reason, r.attemptId(), e);
    }
    return new ScanResult(r.attemptId(), Outcome.DENIED, reason, direction, false);
  }

  private ScanResult replay(AttemptState s) {
    return switch (s.state()) {
      case COMMITTED -> new ScanResult(s.attemptId(), Outcome.ALLOWED, null, null, true);
      case DENIED -> new ScanResult(s.attemptId(), Outcome.DENIED, s.reason(), null, true);
      case GATE_FAILED ->
          new ScanResult(s.attemptId(), Outcome.DENIED, ReasonCode.GATE_HARDWARE_ERROR, null, true);
      case GATE_UNKNOWN ->
          new ScanResult(
              s.attemptId(), Outcome.PENDING, ReasonCode.TURNSTILE_TIMEOUT_RETRY_SCAN, null, true);
      case RESERVED, GATE_COMMANDED, OTHER ->
          new ScanResult(s.attemptId(), Outcome.IN_PROGRESS, ReasonCode.IN_PROGRESS, null, true);
    };
  }
}
