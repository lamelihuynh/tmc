package vn.tmc.userqr.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.Principal;
import vn.tmc.userqr.application.port.in.IDeviceManagement;

/**
 * Device enrollment endpoints (ISP: self-service and HR groups are separate, see
 * api-design-guidelines).
 */
@RestController
public class DeviceController {
  private final IDeviceManagement devices;

  public DeviceController(IDeviceManagement devices) {
    this.devices = devices;
  }

  public record RegisterRequest(@NotBlank String publicKey) {}

  public record RegisterResponse(String deviceId) {}

  @PostMapping("/self-service/v1/devices")
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterResponse register(
      HttpServletRequest http, @Valid @RequestBody RegisterRequest body) {
    var caller = requireCaller(http);
    return new RegisterResponse(
        devices.register(caller, caller.subject(), body.publicKey()).deviceId());
  }

  @DeleteMapping("/self-service/v1/devices/{deviceId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revokeOwn(HttpServletRequest http, @PathVariable String deviceId) {
    devices.revokeOwn(requireCaller(http), deviceId);
  }

  @PostMapping("/hr/v1/employees/{employeeUuid}/devices/revoke")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revokeForEmployee(HttpServletRequest http, @PathVariable UUID employeeUuid) {
    devices.revokeForEmployee(requireCaller(http), employeeUuid);
  }

  private static Principal requireCaller(HttpServletRequest http) {
    var caller = HeaderPrincipalResolver.resolve(http);
    if (caller == null) {
      throw new AccessDeniedException("unauthenticated");
    }
    return caller;
  }
}
