package vn.tmc.auditadmin.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.auditadmin.bootstrap.AuditAdminApplication;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.TestJwtKeys;
import vn.tmc.platform.security.jwt.TestTokens;

/** D-13: audit-admin identifies callers by the identity JWT; dev headers only where enabled. */
@SpringBootTest(classes = AuditAdminApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("inmem")
class BearerTokenApiTest {
  @Autowired MockMvc mvc;

  @DynamicPropertySource
  static void jwt(DynamicPropertyRegistry registry) {
    registry.add("tmc.security.jwks-json", TestJwtKeys::trustedJwksJson);
    registry.add("tmc.security.allow-dev-headers", () -> "false");
  }

  @Test
  void should_let_a_system_admin_token_search_and_verify_the_log() throws Exception {
    var admin = TestTokens.bearer(TestTokens.valid(UUID.randomUUID(), Role.SYSTEM_ADMIN));

    mvc.perform(get("/admin/v1/audit").header("Authorization", admin)).andExpect(status().isOk());
    mvc.perform(get("/admin/v1/audit/verification").header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true));
  }

  @Test
  void should_answer_403_to_a_valid_token_of_another_role() throws Exception {
    var hr = TestTokens.bearer(TestTokens.valid(UUID.randomUUID(), Role.HR_STAFF));

    mvc.perform(get("/admin/v1/audit").header("Authorization", hr))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_answer_401_without_a_valid_token_or_with_dev_headers_off() throws Exception {
    var admin = TestTokens.principal(UUID.randomUUID(), DataScope.all(), Role.SYSTEM_ADMIN);

    mvc.perform(get("/admin/v1/audit"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    mvc.perform(
            get("/admin/v1/audit")
                .header("Authorization", TestTokens.bearer(TestTokens.signedByUntrustedKey(admin))))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/admin/v1/audit")
                .header("X-Subject", UUID.randomUUID().toString())
                .header("X-Roles", "SYSTEM_ADMIN"))
        .andExpect(status().isUnauthorized());
  }
}
