package vn.tmc.userqr.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.TestJwtKeys;
import vn.tmc.platform.security.jwt.TestTokens;
import vn.tmc.userqr.bootstrap.UserQrApplication;
import vn.tmc.userqr.support.PhoneSimulator;

/**
 * C-02 HTTP contract: HR_STAFF (authenticated with Bearer JWT) updates employee status and
 * factories; deactivating revokes the active device and shows up in the sync bundle. Other roles
 * and unauthenticated requests are refused with 403.
 */
@SpringBootTest(classes = UserQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("inmem")
class EmployeeEligibilityApiTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @DynamicPropertySource
  static void jwt(DynamicPropertyRegistry r) {
    r.add("tmc.security.jwks-json", TestJwtKeys::trustedJwksJson);
    r.add("tmc.security.allow-dev-headers", () -> "false");
  }

  @Test
  void should_update_eligibility_revoke_active_device_and_ship_in_sync_bundle() throws Exception {
    var employee = UUID.randomUUID();
    var hr = UUID.randomUUID();
    var phone = new PhoneSimulator(employee);

    var regBody =
        mvc.perform(
                post("/self-service/v1/devices")
                    .header(
                        "Authorization",
                        TestTokens.bearer(TestTokens.valid(employee, Role.EMPLOYEE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"publicKey\":\"" + phone.publicKeyBase64() + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var deviceId = json.readTree(regBody).get("deviceId").asText();

    var before = Instant.now().minusSeconds(1);
    mvc.perform(
            put("/hr/v1/employees/{id}/eligibility", employee)
                .header("Authorization", TestTokens.bearer(TestTokens.valid(hr, Role.HR_STAFF)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\",\"factories\":[\"LA\",\"BD\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.employeeUuid").value(employee.toString()))
        .andExpect(jsonPath("$.status").value("INACTIVE"))
        .andExpect(jsonPath("$.factories[0]").value("BD"))
        .andExpect(jsonPath("$.factories[1]").value("LA"))
        .andExpect(jsonPath("$.updatedAt").isString());

    mvc.perform(get("/internal/v1/sync-bundle").param("since", before.toString()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.employees[?(@.employeeUuid=='" + employee + "')].status").value("INACTIVE"))
        .andExpect(jsonPath("$.deviceKeys[?(@.deviceId=='" + deviceId + "')].revoked").value(true));
  }

  @Test
  void should_refuse_non_hr_callers_and_invalid_status() throws Exception {
    var employee = UUID.randomUUID();
    var hr = UUID.randomUUID();

    mvc.perform(
            put("/hr/v1/employees/{id}/eligibility", employee)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\",\"factories\":[\"BD\"]}"))
        .andExpect(status().isForbidden());

    mvc.perform(
            put("/hr/v1/employees/{id}/eligibility", employee)
                .header(
                    "Authorization", TestTokens.bearer(TestTokens.valid(employee, Role.EMPLOYEE)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\",\"factories\":[\"BD\"]}"))
        .andExpect(status().isForbidden());

    mvc.perform(
            put("/hr/v1/employees/{id}/eligibility", employee)
                .header("Authorization", TestTokens.bearer(TestTokens.valid(hr, Role.HR_STAFF)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SUSPENDED\",\"factories\":[\"BD\"]}"))
        .andExpect(status().isBadRequest());
  }
}
