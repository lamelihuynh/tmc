package vn.tmc.userqr.adapter.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.TestJwtKeys;
import vn.tmc.platform.security.jwt.TestTokens;
import vn.tmc.platform.testsupport.TmcContainers;
import vn.tmc.userqr.adapter.out.messaging.OutboxAuditLog;
import vn.tmc.userqr.application.port.out.IAuditLog;
import vn.tmc.userqr.bootstrap.UserQrApplication;
import vn.tmc.userqr.support.PhoneSimulator;

/**
 * user-qr on the default profile (PostgreSQL, Flyway, Bearer): the HTTP registration ends as one
 * AuditRecorded row in outbox_events. The relay is off so the row stays PENDING for the assertion.
 * Needs Docker: ./gradlew :services:user-qr:integrationTest
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(classes = UserQrApplication.class)
@AutoConfigureMockMvc
class DefaultProfileAuditIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
    r.add("tmc.security.jwks-json", TestJwtKeys::trustedJwksJson);
    r.add("tmc.outbox.relay-enabled", () -> "false");
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired IAuditLog auditLog;

  @Test
  void should_turn_a_registration_over_http_into_one_pending_audit_row() throws Exception {
    assertThat(auditLog).isInstanceOf(OutboxAuditLog.class);
    var employee = UUID.randomUUID();
    var phone = new PhoneSimulator(employee);

    var body =
        mvc.perform(
                post("/self-service/v1/devices")
                    .header(
                        "Authorization",
                        TestTokens.bearer(TestTokens.valid(employee, Role.EMPLOYEE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"publicKey\":\"" + phone.publicKeyBase64() + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var deviceId = json.readTree(body).get("deviceId").asText();

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE publish_status = 'PENDING' "
                    + "AND envelope->'payload'->>'action' = 'DEVICE_REGISTER' "
                    + "AND envelope->'payload'->>'target' = ?",
                Integer.class,
                deviceId))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM devices WHERE device_id = ? AND status = 'ACTIVE'",
                Integer.class,
                deviceId))
        .isEqualTo(1);
  }
}
