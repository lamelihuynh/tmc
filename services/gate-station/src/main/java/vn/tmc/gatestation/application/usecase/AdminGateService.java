package vn.tmc.gatestation.application.usecase;

import java.util.List;
import java.util.Optional;
import vn.tmc.gatestation.application.port.in.IGateManagement;
import vn.tmc.gatestation.application.port.out.IAuditLog;
import vn.tmc.gatestation.application.port.out.IGateConfigRepository;
import vn.tmc.gatestation.domain.GateConfig;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Rbac;
import vn.tmc.platform.security.Role;

/** FR-11 to FR-13, FR-16: only SYSTEM_ADMIN manages the gate catalog. */
public class AdminGateService implements IGateManagement {
  private final IGateConfigRepository gates;
  private final IAuditLog audit;

  public AdminGateService(IGateConfigRepository gates, IAuditLog audit) {
    this.gates = gates;
    this.audit = audit;
  }

  @Override
  public List<GateConfig> list(Principal caller) {
    Rbac.requireRole(caller, Role.SYSTEM_ADMIN);
    return gates.all();
  }

  @Override
  public Optional<GateConfig> find(Principal caller, String gateId) {
    Rbac.requireRole(caller, Role.SYSTEM_ADMIN);
    return gates.find(gateId);
  }

  @Override
  public GateConfig upsert(Principal caller, GateConfig gate) {
    Rbac.requireRole(caller, Role.SYSTEM_ADMIN);
    validate(gate);
    var saved = gates.upsert(gate);
    audit.record("GATE_UPSERTED", caller.subject(), gate.gateId(), auditDetails(saved));
    return saved;
  }

  @Override
  public Optional<GateConfig> setMaintenance(Principal caller, String gateId, boolean maintenance) {
    Rbac.requireRole(caller, Role.SYSTEM_ADMIN);
    return gates
        .find(gateId)
        .map(
            current -> {
              var updated =
                  new GateConfig(
                      current.gateId(),
                      current.factoryCode(),
                      current.hardwareGateId(),
                      current.mode(),
                      maintenance);
              var saved = gates.upsert(updated);
              audit.record(
                  "GATE_MAINTENANCE_CHANGED", caller.subject(), gateId, auditDetails(saved));
              return saved;
            });
  }

  private static String auditDetails(GateConfig gate) {
    return "factoryCode="
        + gate.factoryCode()
        + " hardwareGateId="
        + gate.hardwareGateId()
        + " mode="
        + gate.mode().name()
        + " maintenance="
        + gate.maintenance();
  }

  private static void validate(GateConfig gate) {
    if (gate == null) {
      throw new IllegalArgumentException("gate is required");
    }
    if (gate.gateId() == null || gate.gateId().isBlank()) {
      throw new IllegalArgumentException("gateId is required");
    }
    if (gate.factoryCode() == null || gate.factoryCode().isBlank()) {
      throw new IllegalArgumentException("factoryCode is required");
    }
    if (gate.hardwareGateId() == null || gate.hardwareGateId().isBlank()) {
      throw new IllegalArgumentException("hardwareGateId is required");
    }
    if (gate.mode() == null) {
      throw new IllegalArgumentException("mode is required");
    }
  }
}
