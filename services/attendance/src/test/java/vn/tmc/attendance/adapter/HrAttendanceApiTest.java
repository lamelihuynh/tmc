package vn.tmc.attendance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
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
import vn.tmc.attendance.application.port.in.GateResult;
import vn.tmc.attendance.application.port.in.IAttendanceCommand;
import vn.tmc.attendance.application.port.in.IAttendanceCommand.RecordAttemptCommand;
import vn.tmc.attendance.application.port.out.IAttendanceTransactionRepository;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.bootstrap.AttendanceApplication;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.TestJwtKeys;
import vn.tmc.platform.security.jwt.TestTokens;

/** HR REST, JWT, validation and complete use-case wiring with production-style auth settings. */
@SpringBootTest(classes = AttendanceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"inmem", "test"})
class HrAttendanceApiTest {
  private static final Instant SCANNED_AT = Instant.parse("2026-11-20T07:20:00Z");
  private static final UUID HR = UUID.fromString("0192a1c0-0000-7000-8000-00000000a003");
  private static final Principal CALLER =
      TestTokens.principal(HR, DataScope.all(), Role.HR_STAFF);

  @Autowired MockMvc mvc;
  @Autowired IAttendanceCommand commands;
  @Autowired IAttendanceTransactionRepository transactions;
  @Autowired IOutboxRepository outbox;

  @DynamicPropertySource
  static void security(DynamicPropertyRegistry registry) {
    registry.add("tmc.security.jwks-json", TestJwtKeys::trustedJwksJson);
    registry.add("tmc.security.allow-dev-headers", () -> "false");
  }

  @Test
  void should_create_a_correction_and_return_the_same_result_on_retry() throws Exception {
    var attempt = completed(AttemptSource.ONLINE);
    var correction = UUID.randomUUID();
    var path = "/hr/v1/attendance/" + attempt + "/corrections";
    var body = correctionBody(correction);

    mvc.perform(
            post(path)
                .header("Authorization", bearer())
                .header("X-Correlation-Id", "hr-correction-http")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correction.correctionId").value(correction.toString()))
        .andExpect(jsonPath("$.correction.originalAttemptId").value(attempt.toString()))
        .andExpect(jsonPath("$.correction.previousDirection").value("ENTRY"))
        .andExpect(jsonPath("$.correction.correctedBy").value(HR.toString()))
        .andExpect(jsonPath("$.correction.revision").value(1))
        .andExpect(jsonPath("$.aggregateVersion").value(6))
        .andExpect(jsonPath("$.idempotentReplay").value(false));

    mvc.perform(
            post(path)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correction.correctionId").value(correction.toString()))
        .andExpect(jsonPath("$.idempotentReplay").value(true));

    assertThat(transactions.findByAttemptId(attempt).orElseThrow().direction())
        .isEqualTo(Direction.ENTRY);
    var published =
        outbox.fetchPending(10000).stream()
            .filter(e -> e.eventId().equals(attempt))
            .filter(e -> "AttendanceCorrected".equals(e.eventType()))
            .toList();
    assertThat(published).hasSize(1);
    assertThat(published.getFirst().correlationId()).isEqualTo("hr-correction-http");
  }

  @Test
  void should_derive_the_editor_from_jwt_and_return_the_corrected_last_direction()
      throws Exception {
    var attempt = completed(AttemptSource.ONLINE);
    var original = transactions.findByAttemptId(attempt).orElseThrow();
    var body =
        correctionBody(UUID.randomUUID())
            .replace("\"reason\":", "\"correctedBy\":\"" + UUID.randomUUID() + "\",\"reason\":");

    mvc.perform(
            post("/hr/v1/attendance/{id}/corrections", attempt)
                .header("Authorization", bearer())
                .header("X-Subject", UUID.randomUUID().toString())
                .header("X-Roles", "SYSTEM_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correction.correctedBy").value(HR.toString()));

    mvc.perform(
            get("/internal/v1/employees/{id}/last-direction", original.employeeUuid())
                .param("before", "2026-11-20T07:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.direction").value("EXIT"))
        .andExpect(jsonPath("$.occurredAt").value("2026-11-20T06:55:00Z"));
  }

  @Test
  void should_reject_a_reused_correction_key_with_different_values() throws Exception {
    var attempt = completed(AttemptSource.ONLINE);
    var body = correctionBody(UUID.randomUUID());
    var path = "/hr/v1/attendance/" + attempt + "/corrections";
    mvc.perform(
            post(path)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());
    mvc.perform(
            post(path)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("EXIT", "ENTRY")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("CORRECTION_CONFLICT"));
  }

  @Test
  void should_verify_a_manual_open_once_and_use_the_authenticated_hr_actor() throws Exception {
    var attempt = completed(AttemptSource.MANUAL);
    assertThat(transactions.findByAttemptId(attempt)).isEmpty();

    for (boolean replay : new boolean[] {false, true}) {
      mvc.perform(
              post("/hr/v1/attempts/{id}/verify-manual", attempt)
                  .header("Authorization", bearer())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"reason\":\"SUPERVISOR_CONFIRMED\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.attemptId").value(attempt.toString()))
          .andExpect(jsonPath("$.verifiedBy").value(HR.toString()))
          .andExpect(jsonPath("$.aggregateVersion").value(6))
          .andExpect(jsonPath("$.idempotentReplay").value(replay));
    }
    assertThat(transactions.findByAttemptId(attempt)).isPresent();
    assertThat(
            outbox.fetchPending(10000).stream()
                .filter(e -> e.eventId().equals(attempt))
                .filter(e -> "AttendanceConfirmed".equals(e.eventType())))
        .hasSize(1);
  }

  @Test
  void should_refuse_all_non_hr_roles_for_both_commands() throws Exception {
    var attempt = completed(AttemptSource.ONLINE);
    for (var role : Role.values()) {
      if (role == Role.HR_STAFF) {
        continue;
      }
      var token = TestTokens.bearer(TestTokens.withRoles(CALLER, List.of(role.name())));
      mvc.perform(
              post("/hr/v1/attendance/{id}/corrections", attempt)
                  .header("Authorization", token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(correctionBody(UUID.randomUUID())))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error").value("FORBIDDEN"));
      mvc.perform(
              post("/hr/v1/attempts/{id}/verify-manual", attempt)
                  .header("Authorization", token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"reason\":\"SUPERVISOR_CONFIRMED\"}"))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void should_refuse_scoped_hr_outside_the_factory_and_hide_unknown_attempts() throws Exception {
    var scoped = TestTokens.principal(HR, DataScope.ofGates("LA"), Role.HR_STAFF);
    for (var attempt : List.of(completed(AttemptSource.MANUAL), UUID.randomUUID())) {
      mvc.perform(
              post("/hr/v1/attempts/{id}/verify-manual", attempt)
                  .header("Authorization", TestTokens.bearer(TestTokens.valid(scoped)))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"reason\":\"SUPERVISOR_CONFIRMED\"}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error").value("FORBIDDEN"));
      mvc.perform(
              post("/hr/v1/attendance/{id}/corrections", attempt)
                  .header("Authorization", TestTokens.bearer(TestTokens.valid(scoped)))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(correctionBody(UUID.randomUUID())))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void should_refuse_missing_or_invalid_bearer_tokens_even_with_spoofed_hr_headers()
      throws Exception {
    var attempt = completed(AttemptSource.MANUAL);
    for (var token :
        List.of(
            "",
            "Bearer broken",
            TestTokens.bearer(TestTokens.expired(CALLER)),
            TestTokens.bearer(TestTokens.signedByUntrustedKey(CALLER)),
            TestTokens.bearer(TestTokens.unsigned(CALLER)),
            TestTokens.bearer(TestTokens.withRoles(CALLER, List.of("FAKE_HR"))))) {
      mvc.perform(
              post("/hr/v1/attempts/{id}/verify-manual", attempt)
                  .header("Authorization", token)
                  .header("X-Subject", HR.toString())
                  .header("X-Roles", "HR_STAFF")
                  .header("X-Scope-Factories", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"reason\":\"SUPERVISOR_CONFIRMED\"}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }
    assertThat(transactions.findByAttemptId(attempt)).isEmpty();
  }

  @Test
  void should_return_400_for_missing_fields_free_text_reasons_and_invalid_enums() throws Exception {
    var attempt = completed(AttemptSource.ONLINE);
    for (var body :
        List.of(
            "{}",
            correctionBody(UUID.randomUUID()).replace("WRONG_DIRECTION", "employee name"),
            correctionBody(UUID.randomUUID()).replace("EXIT", "SIDEWAYS"))) {
      mvc.perform(
              post("/hr/v1/attendance/{id}/corrections", attempt)
                  .header("Authorization", bearer())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(
            post("/hr/v1/attempts/{id}/verify-manual", attempt)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_404_for_unknown_attempts_visible_to_global_hr() throws Exception {
    mvc.perform(
            post("/hr/v1/attendance/{id}/corrections", UUID.randomUUID())
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(correctionBody(UUID.randomUUID())))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/hr/v1/attempts/{id}/verify-manual", UUID.randomUUID())
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"SUPERVISOR_CONFIRMED\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_reject_uncounted_corrections_and_verification_of_online_attendance() throws Exception {
    mvc.perform(
            post("/hr/v1/attendance/{id}/corrections", completed(AttemptSource.MANUAL))
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(correctionBody(UUID.randomUUID())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("ATTENDANCE_NOT_COUNTED"));
    mvc.perform(
            post("/hr/v1/attempts/{id}/verify-manual", completed(AttemptSource.ONLINE))
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"SUPERVISOR_CONFIRMED\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("MANUAL_ATTENDANCE_NOT_VERIFIABLE"));
  }

  private UUID completed(AttemptSource source) {
    var attempt = UUID.randomUUID();
    commands.recordAttempt(
        new RecordAttemptCommand(
            attempt,
            UUID.randomUUID(),
            "BD-GATE-01",
            "BD",
            Direction.ENTRY,
            source == AttemptSource.MANUAL ? null : "hr-api-" + attempt,
            source,
            SCANNED_AT,
            "hr-api-setup"));
    commands.markGateCommanded(attempt);
    commands.recordGateResult(attempt, GateResult.OPENED, "hr-api-setup");
    return attempt;
  }

  private String correctionBody(UUID correction) {
    return """
        {"correctionId":"%s","direction":"EXIT","scannedAt":"2026-11-20T06:55:00Z",
         "reason":"WRONG_DIRECTION","correlationId":"hr-correction-body"}
        """
        .formatted(correction);
  }

  private String bearer() {
    return TestTokens.bearer(TestTokens.valid(CALLER));
  }
}
