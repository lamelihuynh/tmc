package vn.tmc.turnstilesim;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
                .content(
                    "{\"direction\":\"ENTRY\",\"trigger_source\":\"QR_SCAN\",\"command_key\":\""
                        + key
                        + "\"}"))
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
                .content(
                    "{\"direction\":\"ENTRY\",\"trigger_source\":\"QR_SCAN\",\"command_key\":\"k2\"}"))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/gates/HW-2/open")
                .header("Idempotency-Key", "i2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"direction\":\"ENTRY\",\"trigger_source\":\"QR_SCAN\",\"command_key\":\"k2\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.replayed").value(true));

    mvc.perform(
        post("/admin/fault")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gateId\":\"HW-3\",\"mode\":\"OFFLINE\"}"));
    mvc.perform(
            post("/gates/HW-3/open")
                .header("Idempotency-Key", "i3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"direction\":\"ENTRY\",\"trigger_source\":\"QR_SCAN\",\"command_key\":\"k3\"}"))
        .andExpect(status().isServiceUnavailable());
    mvc.perform(get("/gates/HW-3/status"))
        .andExpect(jsonPath("$.last_command_key").value("k3"))
        .andExpect(jsonPath("$.last_command_result").value("NOT_OPENED"));
  }

  @Test
  void should_claim_a_key_atomically_per_gate_even_with_concurrent_requests() throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      var first =
          pool.submit(
              () -> {
                start.await();
                return open("HW-CONCURRENT", "same", "c1");
              });
      var second =
          pool.submit(
              () -> {
                start.await();
                return open("HW-CONCURRENT", "same", "c1");
              });
      start.countDown();
      first.get();
      second.get();
      mvc.perform(get("/gates/HW-CONCURRENT/status")).andExpect(jsonPath("$.pass_count").value(1));
      open("HW-ANOTHER", "same", "c2");
      mvc.perform(get("/gates/HW-ANOTHER/status")).andExpect(jsonPath("$.pass_count").value(1));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void should_record_timeout_and_replay_its_http_status() throws Exception {
    mvc.perform(
        post("/admin/fault")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gateId\":\"HW-TIMEOUT\",\"mode\":\"GATEWAY_TIMEOUT\"}"));
    var request =
        post("/gates/HW-TIMEOUT/open")
            .header("Idempotency-Key", "timeout-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"direction\":\"ENTRY\",\"trigger_source\":\"QR_SCAN\",\"command_key\":\"timeout-command\"}");
    mvc.perform(request).andExpect(status().isGatewayTimeout());
    mvc.perform(request)
        .andExpect(status().isGatewayTimeout())
        .andExpect(jsonPath("$.replayed").value(true));
    mvc.perform(get("/gates/HW-TIMEOUT/status"))
        .andExpect(jsonPath("$.last_command_key").value("timeout-command"))
        .andExpect(jsonPath("$.last_command_result").value("NOT_OPENED"));
  }

  @Test
  void should_replay_http_503_when_the_controller_is_offline() throws Exception {
    mvc.perform(
        post("/admin/fault")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gateId\":\"HW-OFFLINE\",\"mode\":\"OFFLINE\"}"));
    var request =
        post("/gates/HW-OFFLINE/open")
            .header("Idempotency-Key", "offline-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"direction\":\"ENTRY\",\"trigger_source\":\"QR_SCAN\",\"command_key\":\"offline-command\"}");

    mvc.perform(request).andExpect(status().isServiceUnavailable());
    mvc.perform(request)
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.replayed").value(true));
    mvc.perform(get("/gates/HW-OFFLINE/status"))
        .andExpect(jsonPath("$.last_command_key").value("offline-command"))
        .andExpect(jsonPath("$.last_command_result").value("NOT_OPENED"))
        .andExpect(jsonPath("$.pass_count").value(0));
  }

  @Test
  void should_reject_incomplete_commands_and_invalid_faults_without_opening() throws Exception {
    mvc.perform(
            post("/gates/HW-INVALID/open")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"command_key\":\"c\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/gates/HW-INVALID/open")
                .header("Idempotency-Key", " ")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"direction\":\"ENTRY\",\"trigger_source\":\"QR_SCAN\",\"command_key\":\"c\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/gates/HW-INVALID/open")
                .header("Idempotency-Key", "valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/admin/fault")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gateId\":\"HW-INVALID\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/admin/fault")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"DELAY_OPENED\",\"delayMs\":-1}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/gates/HW-INVALID/status")).andExpect(jsonPath("$.pass_count").value(0));
  }

  @Test
  void should_reject_invalid_open_fields_before_recording_a_command() throws Exception {
    mvc.perform(
            post("/gates/HW-BAD-KEY/open")
                .header("Idempotency-Key", "bad-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"direction\":\"ENTRY\",\"trigger_source\":\"QR_SCAN\",\"command_key\":\" \"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/gates/HW-BAD-DIRECTION/open")
                .header("Idempotency-Key", "bad-direction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"direction\":\"NORTH\",\"trigger_source\":\"QR_SCAN\",\"command_key\":\"c\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/gates/HW-BAD-TRIGGER/open")
                .header("Idempotency-Key", "bad-trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"direction\":\"ENTRY\",\"trigger_source\":\"AUTOMATIC\",\"command_key\":\"c\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/gates/HW-BAD-OPERATOR/open")
                .header("Idempotency-Key", "bad-operator")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"direction\":\"ENTRY\",\"trigger_source\":\"MANUAL_GUARD_OVERRIDE\",\"operator_id\":\"not-a-uuid\",\"command_key\":\"c\"}"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/gates/HW-BAD-KEY/status"))
        .andExpect(jsonPath("$.last_command_key").value(nullValue()))
        .andExpect(jsonPath("$.pass_count").value(0));
    mvc.perform(get("/gates/HW-BAD-DIRECTION/status"))
        .andExpect(jsonPath("$.last_command_key").value(nullValue()))
        .andExpect(jsonPath("$.pass_count").value(0));
    mvc.perform(get("/gates/HW-BAD-TRIGGER/status"))
        .andExpect(jsonPath("$.last_command_key").value(nullValue()))
        .andExpect(jsonPath("$.pass_count").value(0));
    mvc.perform(get("/gates/HW-BAD-OPERATOR/status"))
        .andExpect(jsonPath("$.last_command_key").value(nullValue()))
        .andExpect(jsonPath("$.pass_count").value(0));
  }

  @Test
  void should_show_close_as_the_last_command() throws Exception {
    open("HW-CLOSE", "close-open", "close-command");
    mvc.perform(post("/gates/HW-CLOSE/close")).andExpect(status().isOk());
    mvc.perform(get("/gates/HW-CLOSE/status"))
        .andExpect(jsonPath("$.last_command_result").value("LOCKED"))
        .andExpect(jsonPath("$.last_command_key").value(nullValue()))
        .andExpect(jsonPath("$.pass_count").value(1));
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
