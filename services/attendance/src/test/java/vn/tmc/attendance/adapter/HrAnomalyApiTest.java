package vn.tmc.attendance.adapter;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.attendance.bootstrap.AttendanceApplication;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.TestJwtKeys;
import vn.tmc.platform.security.jwt.TestTokens;

/** B-03: HR anomaly queue REST endpoint with Bearer JWT verification and HR_STAFF RBAC. */
@SpringBootTest(classes = AttendanceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"inmem", "test"})
class HrAnomalyApiTest {
  @Autowired MockMvc mvc;

  @DynamicPropertySource
  static void jwt(DynamicPropertyRegistry r) {
    r.add("tmc.security.jwks-json", TestJwtKeys::trustedJwksJson);
    r.add("tmc.security.allow-dev-headers", () -> "false");
  }

  @Test
  void should_allow_hr_staff_and_forbid_other_roles_or_unauthenticated_callers() throws Exception {
    var e1 = UUID.randomUUID();
    var e2 = UUID.randomUUID();
    var a1 = UUID.randomUUID();
    var a2 = UUID.randomUUID();
    var a3 = UUID.randomUUID();

    var batch =
        """
        {"kioskId":"BD-KIOSK-01","records":[
          {"attemptId":"%s","journalSeq":1,"employeeUuid":"%s",
           "gateId":"BD-GATE-01","factoryCode":"BD","direction":"ENTRY","jti":"jti-hr-1",
           "scannedAt":"2026-11-20T07:00:00Z","localStatus":"OFFLINE_OPENED"},
          {"attemptId":"%s","journalSeq":2,"employeeUuid":"%s",
           "gateId":"BD-GATE-01","factoryCode":"BD","direction":"ENTRY","jti":"jti-hr-2",
           "scannedAt":"2026-11-20T07:10:00Z","localStatus":"OFFLINE_OPENED"},
          {"attemptId":"%s","journalSeq":3,"employeeUuid":"%s",
           "gateId":"LA-GATE-01","factoryCode":"LA","direction":"ENTRY","jti":"jti-hr-1",
           "scannedAt":"2026-11-20T08:00:00Z","localStatus":"OFFLINE_OPENED"}]}
        """
            .formatted(a1, e1, a2, e1, a3, e2);

    mvc.perform(
            post("/internal/v1/offline-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batch))
        .andExpect(status().isOk());

    String hrToken = TestTokens.bearer(TestTokens.valid(UUID.randomUUID(), Role.HR_STAFF));
    mvc.perform(get("/hr/v1/anomalies").header("Authorization", hrToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].attemptId").value(a2.toString()))
        .andExpect(jsonPath("$[0].anomaly").value("ANTI_PASSBACK_OFFLINE"))
        .andExpect(jsonPath("$[1].attemptId").value(a3.toString()))
        .andExpect(jsonPath("$[1].anomaly").value("TOKEN_REUSED_OFFLINE"));

    mvc.perform(
            get("/hr/v1/anomalies")
                .param("factory", "LA")
                .param("from", "2026-11-20T07:30:00Z")
                .param("to", "2026-11-20T08:30:00Z")
                .header("Authorization", hrToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].attemptId").value(a3.toString()));

    for (Role nonHr : new Role[] {Role.EMPLOYEE, Role.SECURITY_GUARD, Role.PRODUCTION_SUPERVISOR}) {
      mvc.perform(
              get("/hr/v1/anomalies")
                  .header(
                      "Authorization",
                      TestTokens.bearer(TestTokens.valid(UUID.randomUUID(), nonHr))))
          .andExpect(status().isForbidden());
    }

    // Unauthenticated or dev headers when allow-dev-headers=false -> 403
    mvc.perform(get("/hr/v1/anomalies")).andExpect(status().isForbidden());
    mvc.perform(
            get("/hr/v1/anomalies")
                .header("X-Subject", UUID.randomUUID().toString())
                .header("X-Roles", "HR_STAFF"))
        .andExpect(status().isForbidden());
  }
}
