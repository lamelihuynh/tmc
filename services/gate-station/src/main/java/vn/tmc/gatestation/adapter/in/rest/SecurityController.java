package vn.tmc.gatestation.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.tmc.gatestation.application.port.in.IGateAccess.ScanResult;
import vn.tmc.gatestation.application.port.in.IGateManualAccess;
import vn.tmc.gatestation.application.port.in.IGateManualAccess.ManualOpenRequest;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.PrincipalHeaders;

/**
 * Guard Console API (/security/*). Caller identity comes from gateway headers; the use case still
 * checks.
 */
@RestController
public class SecurityController {
  private final IGateManualAccess manual;

  public SecurityController(IGateManualAccess manual) {
    this.manual = manual;
  }

  public record ManualOpenBody(
      @JsonProperty("employee_uuid") @NotNull UUID employeeUuid,
      @NotNull Direction direction,
      @NotBlank String reason) {}

  @PostMapping("/security/v1/gates/{gateId}/manual-open")
  public ScanResult manualOpen(
      HttpServletRequest http,
      @PathVariable String gateId,
      @Valid @RequestBody ManualOpenBody body) {
    var caller =
        PrincipalHeaders.parse(
            http.getHeader("X-Subject"),
            http.getHeader("X-Roles"),
            http.getHeader("X-Scope-Factories"),
            http.getHeader("X-Scope-Gates"));
    if (caller == null) {
      throw new AccessDeniedException("unauthenticated");
    }
    return manual.manualOpen(
        caller,
        new ManualOpenRequest(
            gateId,
            body.employeeUuid(),
            body.direction(),
            body.reason(),
            http.getHeader("X-Correlation-Id")));
  }
}
