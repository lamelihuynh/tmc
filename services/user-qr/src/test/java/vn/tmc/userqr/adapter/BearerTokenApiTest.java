package vn.tmc.userqr.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * user-qr as it runs outside dev: only Bearer tokens from identity count (here signed with the
 * shared test key), the dev headers are ignored.
 */
@SpringBootTest(classes = UserQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("inmem")
class BearerTokenApiTest {
  @Autowired MockMvc mvc;

  @DynamicPropertySource
  static void jwt(DynamicPropertyRegistry r) {
    r.add("tmc.security.jwks-json", TestJwtKeys::trustedJwksJson);
    r.add("tmc.security.allow-dev-headers", () -> "false");
  }

  @Test
  void should_accept_a_valid_bearer_token_and_ignore_dev_headers() throws Exception {
    var employee = UUID.randomUUID();
    var phone = new PhoneSimulator(employee);
    var body = "{\"publicKey\":\"" + phone.publicKeyBase64() + "\"}";

    mvc.perform(
            post("/self-service/v1/devices")
                .header(
                    "Authorization", TestTokens.bearer(TestTokens.valid(employee, Role.EMPLOYEE)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());

    mvc.perform(
            post("/self-service/v1/devices")
                .header("X-Subject", employee.toString())
                .header("X-Roles", "EMPLOYEE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_refuse_a_token_signed_by_another_key() throws Exception {
    var employee = UUID.randomUUID();
    var p = TestTokens.principal(employee, vn.tmc.platform.security.DataScope.all(), Role.EMPLOYEE);

    mvc.perform(
            post("/self-service/v1/devices")
                .header("Authorization", TestTokens.bearer(TestTokens.signedByUntrustedKey(p)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publicKey\":\"x\"}"))
        .andExpect(status().isForbidden());
  }
}
