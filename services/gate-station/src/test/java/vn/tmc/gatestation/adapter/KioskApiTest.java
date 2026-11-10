package vn.tmc.gatestation.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.gatestation.adapter.out.inmem.ProgrammableQrValidation;
import vn.tmc.gatestation.adapter.out.simulated.SimulatedTurnstileAdapter;
import vn.tmc.gatestation.adapter.out.simulated.SimulatedTurnstileAdapter.Fault;
import vn.tmc.gatestation.bootstrap.GateStationApplication;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * The whole service on the inmem profile with the seed gates and employees from application.yml.
 */
@SpringBootTest(classes = GateStationApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("inmem")
class KioskApiTest {
  private static final String E1 = "0192a1c0-0000-7000-8000-000000000001";

  @Autowired MockMvc mvc;
  @Autowired ProgrammableQrValidation qr;
  @Autowired SimulatedTurnstileAdapter turnstile;

  private String body(String gate, String token) {
    return "{\"gate_id\":\"%s\",\"qr_token\":\"%s\",\"scanned_at\":\"%s\"}"
        .formatted(gate, token, Instant.now());
  }

  @Test
  void should_return_200_allowed_then_200_duplicate_for_a_retry_with_the_same_idempotency_key()
      throws Exception {
    qr.accept("http-t1", UUID.fromString(E1), "jti-http-1", Instant.now().plusSeconds(30));
    var key = UUID.randomUUID().toString();

    mvc.perform(
            post("/kiosk/v1/scans")
                .header("Idempotency-Key", key)
                .header("X-Kiosk-Id", "K1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("BD-GATE-01", "http-t1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("ALLOWED"))
        .andExpect(jsonPath("$.direction").value("ENTRY"))
        .andExpect(jsonPath("$.attempt_id").value(key));

    mvc.perform(
            post("/kiosk/v1/scans")
                .header("Idempotency-Key", key)
                .header("X-Kiosk-Id", "K1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("BD-GATE-01", "http-t1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.duplicate").value(true));
  }

  @Test
  void should_return_422_with_a_reason_code_for_a_denial() throws Exception {
    qr.deny("http-bad", ReasonCode.QR_EXPIRED);

    mvc.perform(
            post("/kiosk/v1/scans")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-Kiosk-Id", "K1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("BD-GATE-01", "http-bad")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.outcome").value("DENIED"))
        .andExpect(jsonPath("$.reason_code").value("QR_EXPIRED"));
  }

  @Test
  void should_return_202_when_the_gate_outcome_is_unknown() throws Exception {
    qr.accept(
        "http-slow",
        UUID.fromString("0192a1c0-0000-7000-8000-000000000002"),
        "jti-http-2",
        Instant.now().plusSeconds(30));
    turnstile.setFault(Fault.TIMEOUT_NOT_OPENED);
    try {
      mvc.perform(
              post("/kiosk/v1/scans")
                  .header("Idempotency-Key", UUID.randomUUID().toString())
                  .header("X-Kiosk-Id", "K1")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body("BD-GATE-01", "http-slow")))
          .andExpect(status().isAccepted())
          .andExpect(jsonPath("$.reason_code").value("TURNSTILE_TIMEOUT_RETRY_SCAN"));
    } finally {
      turnstile.setFault(Fault.NONE);
    }
  }

  @Test
  void should_return_400_when_the_idempotency_key_is_not_a_uuid_or_the_body_is_incomplete()
      throws Exception {
    mvc.perform(
            post("/kiosk/v1/scans")
                .header("Idempotency-Key", "not-a-uuid")
                .header("X-Kiosk-Id", "K1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("BD-GATE-01", "x")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/kiosk/v1/scans")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-Kiosk-Id", "K1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_serve_gate_catalog_in_the_sync_bundle_and_accept_heartbeats() throws Exception {
    mvc.perform(get("/kiosk/v1/sync-bundle"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gates[?(@.gateId=='BD-GATE-03')].mode").value("BIDIRECTIONAL"));

    mvc.perform(
            post("/kiosk/v1/heartbeats")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"kioskId\":\"K1\",\"gateId\":\"BD-GATE-01\",\"readerStatus\":\"OK\",\"offlineQueueSize\":0}"))
        .andExpect(status().isNoContent());
  }

  @Test
  void should_ingest_an_offline_batch_through_the_gate_catalog_and_reject_unknown_gates()
      throws Exception {
    var id = UUID.randomUUID();
    var bad = UUID.randomUUID();
    var batch =
        """
        {"kioskId":"K1","records":[
         {"attemptId":"%s","journalSeq":1,"employeeUuid":"%s","gateId":"BD-GATE-01","direction":"ENTRY",
          "jti":"jti-off","scannedAt":"2026-11-09T06:40:00Z","localStatus":"OFFLINE_OPENED"},
         {"attemptId":"%s","journalSeq":2,"employeeUuid":"%s","gateId":"NOPE","direction":"ENTRY",
          "jti":"jti-off2","scannedAt":"2026-11-09T06:41:00Z","localStatus":"OFFLINE_OPENED"}]}
        """
            .formatted(id, E1, bad, E1);

    mvc.perform(
            post("/kiosk/v1/scans/offline-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batch))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.attemptId=='%s')].outcome".formatted(bad)).value("REJECTED"))
        .andExpect(jsonPath("$[?(@.attemptId=='%s')].outcome".formatted(id)).value("ACCEPTED"));
  }

  @Test
  void should_enforce_guard_scope_on_manual_open() throws Exception {
    var guard = UUID.randomUUID().toString();
    var json =
        "{\"employee_uuid\":\"%s\",\"direction\":\"ENTRY\",\"reason\":\"phone broken\"}"
            .formatted(E1);

    mvc.perform(
            post("/security/v1/gates/BD-GATE-01/manual-open")
                .header("X-Subject", guard)
                .header("X-Roles", "SECURITY_GUARD")
                .header("X-Scope-Gates", "BD-GATE-01")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("ALLOWED"));

    mvc.perform(
            post("/security/v1/gates/BD-GATE-02/manual-open")
                .header("X-Subject", guard)
                .header("X-Roles", "SECURITY_GUARD")
                .header("X-Scope-Gates", "BD-GATE-01")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isForbidden());

    mvc.perform(
            post("/security/v1/gates/BD-GATE-01/manual-open")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_answer_gate_observation_for_attendance_reconcile() throws Exception {
    mvc.perform(
            get("/internal/v1/attempts/{id}/gate-observation", UUID.randomUUID())
                .param("gateId", "BD-GATE-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.observation").value("UNDETERMINED"));
  }
}
