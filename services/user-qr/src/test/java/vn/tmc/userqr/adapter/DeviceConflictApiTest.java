package vn.tmc.userqr.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.PrincipalResolver;
import vn.tmc.userqr.adapter.in.rest.ApiExceptionHandler;
import vn.tmc.userqr.adapter.in.rest.DeviceController;
import vn.tmc.userqr.application.port.in.IDeviceManagement;
import vn.tmc.userqr.application.port.out.ActiveDeviceConflictException;

/** C-01: the loser of two parallel registrations gets 409, with no device or employee id. */
class DeviceConflictApiTest {
  private final IDeviceManagement losingRace =
      new IDeviceManagement() {
        @Override
        public RegisteredDevice register(Principal caller, UUID employeeUuid, String key) {
          throw new ActiveDeviceConflictException("another ACTIVE device exists");
        }

        @Override
        public void revokeOwn(Principal caller, String deviceId) {}

        @Override
        public void revokeForEmployee(Principal caller, UUID employeeUuid) {}
      };

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(
              new DeviceController(losingRace, PrincipalResolver.devHeadersOnly()))
          .setControllerAdvice(new ApiExceptionHandler())
          .build();

  @Test
  void should_answer_409_to_the_registration_that_lost_the_race() throws Exception {
    var employee = UUID.randomUUID();

    mvc.perform(
            post("/self-service/v1/devices")
                .header("X-Subject", employee.toString())
                .header("X-Roles", "EMPLOYEE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publicKey\":\"irrelevant\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("CONFLICT"))
        .andExpect(jsonPath("$.detail").value("CONCURRENT_DEVICE_REGISTRATION"));
  }
}
