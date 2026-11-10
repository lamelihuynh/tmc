package vn.tmc.turnstilesim;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TurnstileSimulatorTest {
  @Autowired MockMvc mvc;

  @AfterEach
  void reset() throws Exception {
    mvc.perform(delete("/admin/fault"));
  }

  private String open(String gate, String idem, String key) throws Exception {
    return mvc.perform(
            post("/gates/{g}/open", gate)
                .header("Idempotency-Key", idem)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"direction\":\"ENTRY\",\"command_key\":\"" + key + "\"}"))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void should_unlock_once_per_idempotency_key_even_when_the_command_is_retried() throws Exception {
    open("HW-1", "idem-1", "k1");
    open("HW-1", "idem-1", "k1");

    mvc.perform(get("/gates/HW-1/status"))
        .andExpect(jsonPath("$.pass_count").value(1))
        .andExpect(jsonPath("$.last_command_key").value("k1"))
        .andExpect(jsonPath("$.last_command_result").value("UNLOCKED"));
  }

  @Test
  void should_answer_409_when_the_arm_is_jammed_and_503_when_offline() throws Exception {
    mvc.perform(
        post("/admin/fault")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gateId\":\"HW-2\",\"mode\":\"JAM\"}"));
    mvc.perform(
            post("/gates/HW-2/open")
                .header("Idempotency-Key", "i2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"command_key\":\"k2\"}"))
        .andExpect(status().isConflict());

    mvc.perform(
        post("/admin/fault")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gateId\":\"HW-3\",\"mode\":\"OFFLINE\"}"));
    mvc.perform(
            post("/gates/HW-3/open")
                .header("Idempotency-Key", "i3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"command_key\":\"k3\"}"))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void should_open_but_answer_late_so_the_acknowledgement_can_be_lost() throws Exception {
    mvc.perform(
        post("/admin/fault")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gateId\":\"HW-4\",\"mode\":\"DELAY_OPENED\",\"delayMs\":50}"));

    open("HW-4", "i4", "k4");

    mvc.perform(get("/gates/HW-4/status"))
        .andExpect(jsonPath("$.last_command_result").value("UNLOCKED"))
        .andExpect(jsonPath("$.pass_count").value(1));
  }

  @Test
  void should_report_no_command_for_a_gate_that_never_received_one() throws Exception {
    mvc.perform(get("/gates/HW-NEW/status"))
        .andExpect(jsonPath("$.last_command_key").value(nullValue()))
        .andExpect(jsonPath("$.pass_count").value(0));
  }
}
