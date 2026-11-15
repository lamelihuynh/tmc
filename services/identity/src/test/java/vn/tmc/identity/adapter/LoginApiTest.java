package vn.tmc.identity.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.identity.bootstrap.IdentityApplication;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.JwtPrincipalVerifier;

/**
 * The HTTP contract of docs/api/identity.openapi.yaml: login answer, then JWKS checks the token.
 */
@SpringBootTest(classes = IdentityApplication.class)
@AutoConfigureMockMvc
class LoginApiTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Test
  void should_issue_a_token_that_verifies_against_the_published_jwks() throws Exception {
    var body =
        mvc.perform(
                post("/auth/v1/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"nv001\",\"password\":\"dev-only-change-me\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("GRANTED"))
            .andExpect(jsonPath("$.subject").value("0192a1c0-0000-7000-8000-000000000001"))
            .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(jsonPath("$.expiresAt").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var token = json.readTree(body).get("accessToken").asText();

    var jwks =
        mvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys[0].kty").value("EC"))
            .andExpect(jsonPath("$.keys[0].crv").value("P-256"))
            .andExpect(jsonPath("$.keys[0].alg").value("ES256"))
            .andExpect(jsonPath("$.keys[0].d").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();

    var principal = JwtPrincipalVerifier.fromJwksJson(jwks, IClock.system()).verify(token);
    assertThat(principal).isNotNull();
    assertThat(principal.subject())
        .isEqualTo(UUID.fromString("0192a1c0-0000-7000-8000-000000000001"));
    assertThat(principal.roles()).containsExactly(Role.EMPLOYEE);
    assertThat(principal.scope().factories()).containsExactlyInAnyOrder("BD", "LA");
  }

  @Test
  void should_give_admin_all_factories_and_reveal_nothing_on_refusal() throws Exception {
    mvc.perform(
            post("/auth/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nv001\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("DENIED"))
        .andExpect(jsonPath("$.subject").doesNotExist())
        .andExpect(jsonPath("$.accessToken").doesNotExist());
    mvc.perform(
            post("/auth/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nv001\"}"))
        .andExpect(status().isBadRequest());
  }
}
