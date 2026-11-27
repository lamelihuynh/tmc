package vn.tmc.gatestation.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import vn.tmc.gatestation.bootstrap.GateStationApplication;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.TestJwtKeys;
import vn.tmc.platform.security.jwt.TestTokens;

/** Guard and gate administration APIs accept signed identity tokens outside dev-header mode. */
@SpringBootTest(classes = GateStationApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("inmem")
class BearerTokenApiTest {
  private static final UUID EMPLOYEE = UUID.fromString("0192a1c0-0000-7000-8000-000000000001");
  private static final UUID GUARD = UUID.randomUUID();

  @Autowired MockMvc mvc;

  @DynamicPropertySource
  static void jwt(DynamicPropertyRegistry registry) {
    registry.add("tmc.security.jwks-json", TestJwtKeys::trustedJwksJson);
    registry.add("tmc.security.allow-dev-headers", () -> "false");
  }

  @Test
  void should_accept_a_valid_guard_bearer_token_to_open_a_gate() throws Exception {
    var guard =
        TestTokens.principal(GUARD, DataScope.ofGates("BD", "BD-GATE-01"), Role.SECURITY_GUARD);
    var body =
        "{\"employee_uuid\":\"%s\",\"direction\":\"ENTRY\",\"reason\":\"reader failure\"}"
            .formatted(EMPLOYEE);

    mvc.perform(
            post("/security/v1/gates/BD-GATE-01/manual-open")
                .header("Authorization", TestTokens.bearer(TestTokens.valid(guard)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("ALLOWED"));
  }

  @Test
  void should_accept_a_system_admin_bearer_token_on_gate_administration() throws Exception {
    var admin = TestTokens.valid(UUID.randomUUID(), Role.SYSTEM_ADMIN);

    mvc.perform(get("/admin/v1/gates").header("Authorization", TestTokens.bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.gateId=='BD-GATE-01')]").isNotEmpty());
  }

  @Test
  void should_reject_expired_and_untrusted_tokens_with_401() throws Exception {
    var guard =
        TestTokens.principal(GUARD, DataScope.ofGates("BD", "BD-GATE-01"), Role.SECURITY_GUARD);
    var expired = TestTokens.expired(guard);
    var untrusted = TestTokens.signedByUntrustedKey(guard);
    var body =
        "{\"employee_uuid\":\"%s\",\"direction\":\"ENTRY\",\"reason\":\"reader failure\"}"
            .formatted(EMPLOYEE);

    for (var token : new String[] {expired, untrusted}) {
      mvc.perform(
              post("/security/v1/gates/BD-GATE-01/manual-open")
                  .header("Authorization", TestTokens.bearer(token))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnauthorized());
    }
  }

  @Test
  void should_not_accept_dev_headers_when_the_profile_setting_is_disabled() throws Exception {
    mvc.perform(
            get("/admin/v1/gates")
                .header("X-Subject", UUID.randomUUID().toString())
                .header("X-Roles", "SYSTEM_ADMIN"))
        .andExpect(status().isUnauthorized());
  }
}
