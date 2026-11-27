package vn.tmc.userqr.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.PrincipalResolver;
import vn.tmc.userqr.application.port.in.IEmployeeEligibility;
import vn.tmc.userqr.application.port.in.IEmployeeEligibility.EmployeeEligibilityView;
import vn.tmc.userqr.application.port.in.IEmployeeEligibility.Status;

/**
 * HR Staff endpoint for updating employee eligibility (C-02, docs/api/user-qr.openapi.yaml).
 * Separate from {@link DeviceController} (ISP).
 */
@RestController
public class EmployeeEligibilityController {
  private final IEmployeeEligibility eligibility;
  private final PrincipalResolver principals;

  public EmployeeEligibilityController(
      IEmployeeEligibility eligibility, PrincipalResolver principals) {
    this.eligibility = eligibility;
    this.principals = principals;
  }

  public record UpdateEligibilityRequest(@NotBlank String status, @NotNull Set<String> factories) {}

  @PutMapping("/hr/v1/employees/{employeeUuid}/eligibility")
  public EmployeeEligibilityView update(
      HttpServletRequest http,
      @PathVariable UUID employeeUuid,
      @Valid @RequestBody UpdateEligibilityRequest body) {
    var caller = requireCaller(http);
    var status = parseStatus(body.status());
    return eligibility.update(caller, employeeUuid, status, body.factories());
  }

  private Principal requireCaller(HttpServletRequest http) {
    var caller = principals.resolve(http::getHeader);
    if (caller == null) {
      throw new AccessDeniedException("unauthenticated");
    }
    return caller;
  }

  private static Status parseStatus(String raw) {
    try {
      return Status.valueOf(raw.trim());
    } catch (Exception e) {
      throw new IllegalArgumentException("status must be ACTIVE or INACTIVE");
    }
  }
}
