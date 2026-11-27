package vn.tmc.identity.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.identity.adapter.out.crypto.TotpMfaVerifier;
import vn.tmc.identity.bootstrap.IdentityApplication;
import vn.tmc.platform.kernel.IClock;

/**
 * C-05 over HTTP on the inmem profile: HR_STAFF (hr1) and SYSTEM_ADMIN (admin1) require a real RFC
 * 6238 TOTP code, get a signed JWT when valid, and cannot replay the same code.
 */
@SpringBootTest(classes = IdentityApplication.class, properties = "tmc.outbox.relay-enabled=false")
@ActiveProfiles("inmem")
@AutoConfigureMockMvc
class TotpLoginApiTest {
  private static final String ADMIN_DEV_SECRET = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
  private static final String HR_DEV_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

  @Autowired MockMvc mvc;
  @Autowired IClock clock;

  @Test
  void should_require_totp_for_admin_and_hr_and_refuse_replay() throws Exception {
    mvc.perform(
            post("/auth/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin1\",\"password\":\"dev-only-change-me\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("MFA_REQUIRED"));

    var adminCode = TotpMfaVerifier.generateCodeAt(ADMIN_DEV_SECRET, clock.now());
    mvc.perform(
            post("/auth/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"admin1\",\"password\":\"dev-only-change-me\",\"mfaCode\":\""
                        + adminCode
                        + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("GRANTED"))
        .andExpect(jsonPath("$.roles[0]").value("SYSTEM_ADMIN"))
        .andExpect(jsonPath("$.accessToken").isString());

    // replaying the same code is rejected
    mvc.perform(
            post("/auth/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"admin1\",\"password\":\"dev-only-change-me\",\"mfaCode\":\""
                        + adminCode
                        + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("DENIED"));

    var hrCode = TotpMfaVerifier.generateCodeAt(HR_DEV_SECRET, clock.now());
    mvc.perform(
            post("/auth/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"hr1\",\"password\":\"dev-only-change-me\",\"mfaCode\":\""
                        + hrCode
                        + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("GRANTED"))
        .andExpect(jsonPath("$.roles[0]").value("HR_STAFF"))
        .andExpect(jsonPath("$.accessToken").isString());
  }
}
