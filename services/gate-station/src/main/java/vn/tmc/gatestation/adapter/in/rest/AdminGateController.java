package vn.tmc.gatestation.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.tmc.gatestation.application.port.in.IGateManagement;
import vn.tmc.gatestation.domain.GateConfig;
import vn.tmc.gatestation.domain.GateMode;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.PrincipalResolver;

/** Gate catalog administration (FR-11 to FR-13, FR-16). Guarded by Rbac, not by the gateway. */
@RestController
public class AdminGateController {
  private final IGateManagement gates;
  private final PrincipalResolver principals;

  public AdminGateController(IGateManagement gates, PrincipalResolver principals) {
    this.gates = gates;
    this.principals = principals;
  }

  public record GateBody(
      @NotBlank String factoryCode,
      @NotBlank String hardwareGateId,
      @NotNull GateMode mode,
      boolean maintenance) {}

  public record MaintenanceBody(@NotNull Boolean maintenance) {}

  @GetMapping("/admin/v1/gates")
  public List<GateConfig> list(HttpServletRequest http) {
    return gates.list(requireCaller(http));
  }

  @GetMapping("/admin/v1/gates/{gateId}")
  public ResponseEntity<GateConfig> find(HttpServletRequest http, @PathVariable String gateId) {
    return gates
        .find(requireCaller(http), gateId)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PutMapping("/admin/v1/gates/{gateId}")
  public GateConfig upsert(
      HttpServletRequest http, @PathVariable String gateId, @Valid @RequestBody GateBody body) {
    return gates.upsert(
        requireCaller(http),
        new GateConfig(
            gateId, body.factoryCode(), body.hardwareGateId(), body.mode(), body.maintenance()));
  }

  @PatchMapping("/admin/v1/gates/{gateId}/maintenance")
  public ResponseEntity<GateConfig> setMaintenance(
      HttpServletRequest http,
      @PathVariable String gateId,
      @Valid @RequestBody MaintenanceBody body) {
    return gates
        .setMaintenance(requireCaller(http), gateId, body.maintenance())
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private Principal requireCaller(HttpServletRequest http) {
    var caller = principals.resolve(http::getHeader);
    if (caller == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthenticated");
    }
    return caller;
  }
}
