package vn.tmc.identity.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.identity.bootstrap.IdentityApplication;
import vn.tmc.platform.messaging.outbox.OutboxStore;

/**
 * C-08 wired end to end on the inmem profile: wrong passwords over HTTP end as AuditRecorded rows
 * in identity's outbox, with the lock event and without the password or username. The relay is off
 * so the rows stay pending for the assertions.
 */
@SpringBootTest(classes = IdentityApplication.class, properties = "tmc.outbox.relay-enabled=false")
@ActiveProfiles("inmem")
@AutoConfigureMockMvc
class LoginAuditApiTest {
  static final String NV002 = "0192a1c0-0000-7000-8000-000000000002";

  @Autowired MockMvc mvc;
  @Autowired OutboxStore outbox;
  @Autowired ObjectMapper json;

  private void login(String password, int expectedStatus) throws Exception {
    mvc.perform(
            post("/auth/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nv002\",\"password\":\"" + password + "\"}"))
        .andExpect(status().is(expectedStatus));
  }

  @Test
  void should_put_the_lock_event_of_five_wrong_passwords_in_the_outbox() throws Exception {
    for (int i = 0; i < 4; i++) {
      login("not-the-password", 401);
    }
    login("not-the-password", 423);

    var events = outbox.fetchPending(1000);
    assertThat(events)
        .filteredOn(e -> NV002.equals(e.payload().get("target")))
        .extracting(e -> e.payload().get("action"))
        .containsExactly(
            "LOGIN_FAILED",
            "LOGIN_FAILED",
            "LOGIN_FAILED",
            "LOGIN_FAILED",
            "LOGIN_FAILED",
            "LOGIN_LOCKED");
    assertThat(events).allSatisfy(e -> assertThat(e.source()).isEqualTo("identity"));
    assertThat(json.writeValueAsString(events)).doesNotContain("not-the-password", "nv002");
  }
}
