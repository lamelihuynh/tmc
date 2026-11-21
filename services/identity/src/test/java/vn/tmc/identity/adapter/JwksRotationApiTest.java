package vn.tmc.identity.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
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
import vn.tmc.identity.bootstrap.IdentityApplication;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.JwtPrincipalVerifier;
import vn.tmc.platform.security.jwt.TestJwtKeys;
import vn.tmc.platform.security.jwt.TestTokens;

/**
 * C-12 over HTTP: identity configured mid-rotation (new key test-key-b signs, old key test-key-a
 * still published) serves both kids, and a verifier built from that JWKS accepts tokens of both.
 */
@SpringBootTest(classes = IdentityApplication.class)
@ActiveProfiles("inmem")
@AutoConfigureMockMvc
class JwksRotationApiTest {
  @DynamicPropertySource
  static void keys(DynamicPropertyRegistry r) {
    r.add("tmc.identity.jwt.private-jwk", () -> TestJwtKeys.UNTRUSTED_PRIVATE_JWK);
    r.add(
        "tmc.identity.jwt.previous-public-jwk",
        () -> TestJwtKeys.TRUSTED.publicJwk().toJSONString());
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Test
  void should_serve_two_kids_and_accept_tokens_of_the_old_and_the_new_key() throws Exception {
    var jwks =
        mvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys.length()").value(2))
            .andExpect(jsonPath("$.keys[0].kid").value("test-key-b"))
            .andExpect(jsonPath("$.keys[1].kid").value("test-key-a"))
            .andExpect(jsonPath("$.keys[*].d").isEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var verifier = JwtPrincipalVerifier.fromJwksJson(jwks, IClock.system());

    var body =
        mvc.perform(
                post("/auth/v1/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"nv001\",\"password\":\"dev-only-change-me\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var newToken = json.readTree(body).get("accessToken").asText();
    var oldToken =
        TestTokens.valid(new Principal(UUID.randomUUID(), Set.of(Role.EMPLOYEE), DataScope.all()));

    assertThat(verifier.verify(newToken)).isNotNull();
    assertThat(verifier.verify(oldToken)).isNotNull();
  }
}
