package vn.tmc.reporting.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.TestJwtKeys;
import vn.tmc.platform.security.jwt.TestTokens;
import vn.tmc.reporting.bootstrap.ReportingApplication;

/** D-13: reporting identifies callers by the identity JWT; dev headers only where enabled. */
@SpringBootTest(classes = ReportingApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("inmem")
class BearerTokenApiTest {
  @Autowired MockMvc mvc;

  @DynamicPropertySource
  static void jwt(DynamicPropertyRegistry registry) {
    registry.add("tmc.security.jwks-json", TestJwtKeys::trustedJwksJson);
    registry.add("tmc.security.allow-dev-headers", () -> "false");
  }

  private static final DataScope BD = new DataScope(Set.of("BD"), Set.of(), Set.of(), false);

  @Test
  void should_answer_a_valid_supervisor_token_within_its_factory() throws Exception {
    var token =
        TestTokens.valid(TestTokens.principal(UUID.randomUUID(), BD, Role.PRODUCTION_SUPERVISOR));

    mvc.perform(
            get("/supervision/v1/presence")
                .param("factory", "BD")
                .header("Authorization", TestTokens.bearer(token)))
        .andExpect(status().isOk());
    mvc.perform(
            get("/supervision/v1/alerts")
                .param("factory", "BD")
                .header("Authorization", TestTokens.bearer(token)))
        .andExpect(status().isOk());
  }

  @Test
  void should_answer_403_for_a_valid_token_outside_its_scope() throws Exception {
    var token =
        TestTokens.valid(TestTokens.principal(UUID.randomUUID(), BD, Role.PRODUCTION_SUPERVISOR));

    mvc.perform(
            get("/supervision/v1/presence")
                .param("factory", "LA")
                .header("Authorization", TestTokens.bearer(token)))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_answer_401_without_a_token_or_with_an_expired_or_untrusted_one() throws Exception {
    var supervisor = TestTokens.principal(UUID.randomUUID(), BD, Role.PRODUCTION_SUPERVISOR);

    mvc.perform(get("/supervision/v1/presence").param("factory", "BD"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    for (var token :
        new String[] {
          TestTokens.expired(supervisor), TestTokens.signedByUntrustedKey(supervisor), "garbage"
        }) {
      mvc.perform(
              get("/supervision/v1/presence")
                  .param("factory", "BD")
                  .header("Authorization", TestTokens.bearer(token)))
          .andExpect(status().isUnauthorized());
    }
  }

  @Test
  void should_not_accept_dev_headers_when_they_are_off() throws Exception {
    mvc.perform(
            get("/supervision/v1/presence")
                .param("factory", "BD")
                .header("X-Subject", UUID.randomUUID().toString())
                .header("X-Roles", "PRODUCTION_SUPERVISOR")
                .header("X-Scope-Factories", "BD"))
        .andExpect(status().isUnauthorized());
  }
}
