package vn.tmc.attendance.adapter;

import static org.hamcrest.Matchers.containsString;
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
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.attendance.bootstrap.AttendanceApplication;

/**
 * Whole service on the inmem profile: proves the wiring, JSON contract and idempotent REST
 * behaviour.
 */
@SpringBootTest(classes = AttendanceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"inmem", "test"})
class InternalApiFlowTest {
  @Autowired MockMvc mvc;

  private String attemptJson(UUID id) {
    return """
        {"attemptId":"%s","employeeUuid":"0192a1c0-0000-7000-8000-000000000001","gateId":"BD-GATE-01",
         "factoryCode":"BD","direction":"ENTRY","jti":"jti-rest-1","scannedAt":"2026-11-09T06:45:00Z",
         "correlationId":"corr-rest"}
        """
        .formatted(id);
  }

  @Test
  void should_walk_record_command_result_and_stay_idempotent_on_replay() throws Exception {
    var id = UUID.randomUUID();

    mvc.perform(
            post("/internal/v1/attempts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(attemptJson(id)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RESERVED"))
        .andExpect(jsonPath("$.duplicate").value(false));

    mvc.perform(
            post("/internal/v1/attempts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(attemptJson(id)))
        .andExpect(jsonPath("$.duplicate").value(true));

    mvc.perform(post("/internal/v1/attempts/{id}/gate-commanded", id))
        .andExpect(jsonPath("$.status").value("GATE_COMMANDED"));

    mvc.perform(
            post("/internal/v1/attempts/{id}/gate-result", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"result\":\"OPENED\",\"correlationId\":\"c\"}"))
        .andExpect(jsonPath("$.status").value("COMMITTED"));

    mvc.perform(get("/internal/v1/attempts/{id}", id))
        .andExpect(jsonPath("$.status").value("COMMITTED"));
  }

  @Test
  void should_return_404_for_an_unknown_attempt() throws Exception {
    mvc.perform(post("/internal/v1/attempts/{id}/gate-commanded", UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_return_409_when_a_result_arrives_before_the_gate_was_commanded() throws Exception {
    var id = UUID.randomUUID();
    mvc.perform(
        post("/internal/v1/attempts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(attemptJson(id)));

    mvc.perform(
            post("/internal/v1/attempts/{id}/gate-result", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"result\":\"OPENED\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value(containsString("RESERVED")));
  }

  @Test
  void should_report_per_record_outcomes_for_an_offline_batch() throws Exception {
    var id = UUID.randomUUID();
    var body =
        """
        {"kioskId":"BD-KIOSK-01","records":[
          {"attemptId":"%s","journalSeq":1,"employeeUuid":"0192a1c0-0000-7000-8000-000000000009",
           "gateId":"BD-GATE-01","factoryCode":"BD","direction":"ENTRY","jti":"jti-off-1",
           "scannedAt":"2026-11-09T06:40:00Z","localStatus":"OFFLINE_OPENED"},
          {"attemptId":"%s","journalSeq":2,"employeeUuid":"0192a1c0-0000-7000-8000-000000000009",
           "gateId":"BD-GATE-01","factoryCode":"BD","direction":"ENTRY","jti":"jti-off-1",
           "scannedAt":"2026-11-09T06:40:00Z","localStatus":"OFFLINE_OPENED"}]}
        """
            .formatted(id, id);

    mvc.perform(
            post("/internal/v1/offline-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].outcome").value("ACCEPTED"))
        .andExpect(jsonPath("$[1].outcome").value("DUPLICATE"));
  }

  @Test
  void should_reject_a_request_that_misses_required_fields() throws Exception {
    mvc.perform(post("/internal/v1/attempts").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }
}
