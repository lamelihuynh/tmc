package vn.tmc.userqr.adapter.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.TestJwtKeys;
import vn.tmc.platform.security.jwt.TestTokens;
import vn.tmc.platform.testsupport.TmcContainers;
import vn.tmc.userqr.adapter.out.persistence.JdbcEmployeeDirectory;
import vn.tmc.userqr.application.port.out.IEmployeeDirectory;
import vn.tmc.userqr.bootstrap.UserQrApplication;
import vn.tmc.userqr.support.PhoneSimulator;

/**
 * C-02 on PostgreSQL (default profile, Flyway V3__employees.sql, Bearer auth, durable outbox):
 * verifies seed loading from docs/api/fixtures/employee-seed.json, HR update persistence, device
 * revocation on deactivation, sync-bundle delta, saveIfAbsent durability, and AuditRecorded rows in
 * outbox_events (including DENIED attempts). Needs Docker: ./gradlew
 * :services:user-qr:integrationTest
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(classes = UserQrApplication.class)
@AutoConfigureMockMvc
@Import(EmployeeDirectoryIT.ClockConfig.class)
class EmployeeDirectoryIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();

  static final AtomicReference<Instant> NOW =
      new AtomicReference<>(Instant.now().truncatedTo(ChronoUnit.MICROS));

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
    r.add("tmc.security.jwks-json", TestJwtKeys::trustedJwksJson);
    r.add("tmc.outbox.relay-enabled", () -> "false");
  }

  @TestConfiguration
  static class ClockConfig {
    @Bean
    @Primary
    IClock testClock() {
      return NOW::get;
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired IEmployeeDirectory directory;

  @Test
  void should_seed_employees_persist_hr_updates_revoke_device_and_write_audit_outbox()
      throws Exception {
    NOW.set(Instant.now().truncatedTo(ChronoUnit.MICROS));
    assertThat(directory).isInstanceOf(JdbcEmployeeDirectory.class);
    var seedCount =
        json.readTree(Path.of("../../docs/api/fixtures/employee-seed.json").toFile())
            .get("employees")
            .size();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM employees", Integer.class))
        .isGreaterThanOrEqualTo(seedCount);

    var employee = UUID.randomUUID();
    var hr = UUID.randomUUID();
    var hrBearer = TestTokens.bearer(TestTokens.valid(hr, Role.HR_STAFF));

    // create active employee via HR endpoint
    mvc.perform(
            put("/hr/v1/employees/{id}/eligibility", employee)
                .header("Authorization", hrBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\",\"factories\":[\"BD\",\"LA\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    // employee registers a device
    var phone = new PhoneSimulator(employee);
    var regBody =
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
    var deviceId = json.readTree(regBody).get("deviceId").asText();

    var cursor = NOW.get();
    NOW.updateAndGet(t -> t.plusSeconds(2));

    // HR deactivates employee: updates employees table, revokes device, writes outbox audit rows
    mvc.perform(
            put("/hr/v1/employees/{id}/eligibility", employee)
                .header("Authorization", hrBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\",\"factories\":[\"BD\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"))
        .andExpect(jsonPath("$.factories[0]").value("BD"));

    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM employees WHERE employee_uuid = ?", String.class, employee))
        .isEqualTo("INACTIVE");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM devices WHERE device_id = ?", String.class, deviceId))
        .isEqualTo("REVOKED");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE publish_status = 'PENDING' "
                    + "AND envelope->'payload'->>'action' = 'EMPLOYEE_ELIGIBILITY_UPDATE' "
                    + "AND envelope->'payload'->>'target' = ? "
                    + "AND envelope->'payload'->>'result' = 'INACTIVE'",
                Integer.class,
                employee.toString()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE publish_status = 'PENDING' "
                    + "AND envelope->'payload'->>'action' = 'DEVICE_REVOKE_BY_STAFF' "
                    + "AND envelope->'payload'->>'target' = ?",
                Integer.class,
                deviceId))
        .isEqualTo(1);

    // start-up seed re-run (saveIfAbsent) must never overwrite durable HR changes
    directory.saveIfAbsent(
        new IEmployeeDirectory.Eligibility(
            employee, true, Set.of("BD", "LA"), NOW.updateAndGet(t -> t.plusSeconds(2))));
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM employees WHERE employee_uuid = ?", String.class, employee))
        .isEqualTo("INACTIVE");

    mvc.perform(get("/internal/v1/sync-bundle").param("since", cursor.toString()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.employees[?(@.employeeUuid=='" + employee + "')].status").value("INACTIVE"))
        .andExpect(jsonPath("$.deviceKeys[?(@.deviceId=='" + deviceId + "')].revoked").value(true));
  }

  @Test
  void should_refuse_non_hr_caller_and_keep_denied_audit_row_in_outbox() throws Exception {
    NOW.set(Instant.now().truncatedTo(ChronoUnit.MICROS));
    var caller = UUID.randomUUID();
    var target = UUID.randomUUID();

    mvc.perform(
            put("/hr/v1/employees/{id}/eligibility", target)
                .header("Authorization", TestTokens.bearer(TestTokens.valid(caller, Role.EMPLOYEE)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\",\"factories\":[\"BD\"]}"))
        .andExpect(status().isForbidden());

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM employees WHERE employee_uuid = ?", Integer.class, target))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE publish_status = 'PENDING' "
                    + "AND envelope->'payload'->>'action' = 'EMPLOYEE_ELIGIBILITY_UPDATE' "
                    + "AND envelope->'payload'->>'target' = ? "
                    + "AND envelope->'payload'->>'actor' = ? "
                    + "AND envelope->'payload'->>'result' = 'DENIED'",
                Integer.class,
                target.toString(),
                caller.toString()))
        .isEqualTo(1);
  }
}
