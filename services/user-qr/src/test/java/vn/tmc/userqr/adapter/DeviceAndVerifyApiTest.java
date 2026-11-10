package vn.tmc.userqr.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.userqr.bootstrap.UserQrApplication;
import vn.tmc.userqr.support.PhoneSimulator;

/**
 * End to end over HTTP on the inmem profile: enroll a phone, verify its token, revoke, verify
 * again.
 */
@SpringBootTest(classes = UserQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("inmem")
class DeviceAndVerifyApiTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Test
  void should_enroll_verify_revoke_and_expose_the_revocation_in_the_sync_bundle() throws Exception {
    var employee = UUID.randomUUID();
    var phone = new PhoneSimulator(employee);

    var registered =
        mvc.perform(
                post("/self-service/v1/devices")
                    .header("X-Subject", employee.toString())
                    .header("X-Roles", "EMPLOYEE")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"publicKey\":\"" + phone.publicKeyBase64() + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode body = json.readTree(registered.getResponse().getContentAsString());
    phone.deviceId = body.get("deviceId").asText();

    var token = phone.token(Instant.now(), Instant.now().plusSeconds(30), "jti-http-1");
    mvc.perform(
            post("/internal/v1/qr/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true))
        .andExpect(jsonPath("$.employeeUuid").value(employee.toString()));

    mvc.perform(
            delete("/self-service/v1/devices/{id}", phone.deviceId)
                .header("X-Subject", employee.toString())
                .header("X-Roles", "EMPLOYEE"))
        .andExpect(status().isNoContent());

    mvc.perform(
            post("/internal/v1/qr/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
        .andExpect(jsonPath("$.valid").value(false))
        .andExpect(jsonPath("$.reason").value("DEVICE_REVOKED"));

    mvc.perform(get("/internal/v1/sync-bundle"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.deviceKeys[?(@.deviceId=='" + phone.deviceId + "')].revoked").value(true));
  }

  @Test
  void should_return_403_without_credentials_and_for_the_wrong_role() throws Exception {
    mvc.perform(
            post("/self-service/v1/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publicKey\":\"x\"}"))
        .andExpect(status().isForbidden());

    mvc.perform(
            post("/hr/v1/employees/{id}/devices/revoke", UUID.randomUUID())
                .header("X-Subject", UUID.randomUUID().toString())
                .header("X-Roles", "EMPLOYEE"))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_return_400_for_a_public_key_that_is_not_an_ec_key() throws Exception {
    var employee = UUID.randomUUID();
    mvc.perform(
            post("/self-service/v1/devices")
                .header("X-Subject", employee.toString())
                .header("X-Roles", "EMPLOYEE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publicKey\":\"bm90LWEta2V5\"}"))
        .andExpect(status().isBadRequest());
  }
}
