package vn.tmc.gatestation.application.usecase;

import vn.tmc.gatestation.application.port.in.IGateAccess.Outcome;
import vn.tmc.gatestation.application.port.in.IGateAccess.ScanResult;
import vn.tmc.gatestation.application.port.in.IGateManualAccess;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.AttemptRecord;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.GateResult;
import vn.tmc.gatestation.application.port.out.IAuditLog;
import vn.tmc.gatestation.application.port.out.IEmployeeDirectory;
import vn.tmc.gatestation.application.port.out.IGateConfigRepository;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.OpenGateRequest;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.Trigger;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.kernel.ReasonCode;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Rbac;
import vn.tmc.platform.security.Role;

/**
 * UC-06 / FR-20. Only a Security Guard assigned to this gate, with a mandatory reason, for an
 * active employee. The pass is recorded as MANUAL and the audit entry always keeps the reason
 * (NFR-02).
 */
public class ManualOpenUseCase implements IGateManualAccess {
  private final IGateConfigRepository gates;
  private final IEmployeeDirectory employees;
  private final IAttendanceGateway attendance;
  private final ITurnstileControlPort turnstile;
  private final IAuditLog audit;
  private final IClock clock;

  public ManualOpenUseCase(
      IGateConfigRepository gates,
      IEmployeeDirectory employees,
      IAttendanceGateway attendance,
      ITurnstileControlPort turnstile,
      IAuditLog audit,
      IClock clock) {
    this.gates = gates;
    this.employees = employees;
    this.attendance = attendance;
    this.turnstile = turnstile;
    this.audit = audit;
    this.clock = clock;
  }

  @Override
  public ScanResult manualOpen(Principal guard, ManualOpenRequest request) {
    Rbac.requireRole(guard, Role.SECURITY_GUARD);
    Rbac.requireGate(guard, request.gateId());
    if (request.reason() == null || request.reason().isBlank()) {
      throw new IllegalArgumentException("reason is required for a manual open");
    }
    var gate =
        gates
            .find(request.gateId())
            .orElseThrow(() -> new IllegalArgumentException("unknown gate"));
    var attemptId = Ids.uuidV7();

    boolean active = employees.find(request.employeeUuid()).map(e -> e.active()).orElse(false);
    if (!active) {
      audit.record(
          "MANUAL_OPEN_REFUSED",
          guard.subject(),
          request.gateId(),
          "inactive employee: " + request.reason());
      return new ScanResult(
          attemptId, Outcome.DENIED, ReasonCode.EMPLOYEE_INACTIVE, request.direction(), false);
    }

    audit.record("MANUAL_OPEN_REQUESTED", guard.subject(), request.gateId(), request.reason());
    attendance.recordAttempt(
        new AttemptRecord(
            attemptId,
            request.employeeUuid(),
            gate.gateId(),
            gate.factoryCode(),
            request.direction(),
            "manual-" + attemptId,
            "MANUAL",
            clock.now(),
            request.correlationId()));
    attendance.markGateCommanded(attemptId);

    var result =
        turnstile.openGate(
            new OpenGateRequest(
                gate.hardwareGateId(),
                request.direction(),
                attemptId.toString(),
                Trigger.MANUAL_GUARD_OVERRIDE,
                guard.subject(),
                request.correlationId()));

    return switch (result) {
      case OPENED -> {
        attendance.recordGateResult(attemptId, GateResult.OPENED, request.correlationId());
        audit.record("MANUAL_OPEN_DONE", guard.subject(), request.gateId(), attemptId.toString());
        yield new ScanResult(attemptId, Outcome.ALLOWED, null, request.direction(), false);
      }
      case ERROR -> {
        attendance.recordGateResult(attemptId, GateResult.ERROR, request.correlationId());
        yield new ScanResult(
            attemptId, Outcome.DENIED, ReasonCode.GATE_HARDWARE_ERROR, request.direction(), false);
      }
      case TIMEOUT -> {
        attendance.recordGateResult(attemptId, GateResult.UNKNOWN, request.correlationId());
        yield new ScanResult(
            attemptId,
            Outcome.PENDING,
            ReasonCode.TURNSTILE_TIMEOUT_RETRY_SCAN,
            request.direction(),
            false);
      }
    };
  }
}
