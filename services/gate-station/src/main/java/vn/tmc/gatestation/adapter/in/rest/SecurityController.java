package vn.tmc.gatestation.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.tmc.gatestation.application.port.in.IGateAccess.ScanResult;
import vn.tmc.gatestation.application.port.in.IGateManualAccess;
import vn.tmc.gatestation.application.port.in.IGateManualAccess.ManualOpenRequest;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.platform.security.PrincipalResolver;

/**
 * Guard Console API (/security/*). PrincipalResolver verifies Bearer JWTs; dev headers are accepted
 * only in the local and inmem profiles. The use case still checks authorization.
 */
@RestController
public class SecurityController {
  private final IGateManualAccess manual;
  private final PrincipalResolver principals;

  public SecurityController(IGateManualAccess manual, PrincipalResolver principals) {
    this.manual = manual;
    this.principals = principals;
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
    var caller = principals.resolve(http::getHeader);
    if (caller == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthenticated");
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
