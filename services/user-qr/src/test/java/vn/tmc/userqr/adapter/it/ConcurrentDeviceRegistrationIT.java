package vn.tmc.userqr.adapter.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import org.springframework.mock.web.MockHttpServletResponse;
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
import vn.tmc.userqr.adapter.out.persistence.JdbcDeviceKeyRegistry;
import vn.tmc.userqr.application.port.out.IDeviceKeyRegistry;
import vn.tmc.userqr.bootstrap.UserQrApplication;
import vn.tmc.userqr.domain.Device;
import vn.tmc.userqr.support.PhoneSimulator;

/**
 * C-01: two registrations for the same employee at the same time, over HTTP on the default profile
 * (PostgreSQL, real transactions). Both requests are held right after they read the active device,
 * so both really race to insert: exactly one wins (201), the other gets 409, and only one ACTIVE
 * device remains. Needs Docker: ./gradlew :services:user-qr:integrationTest
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(classes = UserQrApplication.class)
@AutoConfigureMockMvc
@Import(ConcurrentDeviceRegistrationIT.Rendezvous.class)
class ConcurrentDeviceRegistrationIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();

  static final AtomicReference<CyclicBarrier> BARRIER = new AtomicReference<>();

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
    r.add("tmc.security.jwks-json", TestJwtKeys::trustedJwksJson);
    r.add("tmc.outbox.relay-enabled", () -> "false");
  }

  /** The real JDBC registry, made to wait for the other request when a barrier is armed. */
  @TestConfiguration
  static class Rendezvous {
    @Bean
    @Primary
    IDeviceKeyRegistry rendezvousRegistry(JdbcTemplate jdbc) {
      var real = new JdbcDeviceKeyRegistry(jdbc);
      return new IDeviceKeyRegistry() {
        @Override
        public Optional<Device> findById(String deviceId) {
          return real.findById(deviceId);
        }

        @Override
        public Optional<Device> findActiveByEmployee(UUID employeeUuid) {
          var result = real.findActiveByEmployee(employeeUuid);
          var barrier = BARRIER.get();
          if (barrier != null) {
            try {
              barrier.await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
              // a lone caller simply goes on
            }
          }
          return result;
        }

        @Override
        public void save(Device device) {
          real.save(device);
        }

        @Override
        public List<Device> changedSince(Instant since) {
          return real.changedSince(since);
        }
      };
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;

  private Callable<MockHttpServletResponse> register(UUID employee) {
    var body = "{\"publicKey\":\"" + new PhoneSimulator(employee).publicKeyBase64() + "\"}";
    var bearer = TestTokens.bearer(TestTokens.valid(employee, Role.EMPLOYEE));
    return () ->
        mvc.perform(
                post("/self-service/v1/devices")
                    .header("Authorization", bearer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andReturn()
            .getResponse();
  }

  private List<MockHttpServletResponse> runParallelRegistrations(UUID employee) throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    List<MockHttpServletResponse> responses = new ArrayList<>();
    BARRIER.set(new CyclicBarrier(2));
    try {
      var a = pool.submit(register(employee));
      var b = pool.submit(register(employee));
      responses.add(a.get(30, TimeUnit.SECONDS));
      responses.add(b.get(30, TimeUnit.SECONDS));
    } finally {
      BARRIER.set(null);
      pool.shutdownNow();
    }
    return responses;
  }

  @Test
  void should_let_exactly_one_of_two_parallel_registrations_win_and_answer_409_to_the_other()
      throws Exception {
    var employee = UUID.randomUUID();

    var responses = runParallelRegistrations(employee);

    assertThat(responses)
        .extracting(MockHttpServletResponse::getStatus)
        .containsExactlyInAnyOrder(201, 409);
    var winner = responses.stream().filter(r -> r.getStatus() == 201).findFirst().orElseThrow();
    var loser = responses.stream().filter(r -> r.getStatus() == 409).findFirst().orElseThrow();
    var winnerDevice = json.readTree(winner.getContentAsString()).get("deviceId").asText();
    assertThat(json.readTree(loser.getContentAsString()).get("error").asText())
        .isEqualTo("CONFLICT");

    // the loser left nothing behind: one device, ACTIVE, the winner's
    assertThat(
            jdbc.queryForList(
                "SELECT device_id FROM devices WHERE employee_uuid = ? AND status = 'ACTIVE'",
                String.class,
                employee))
        .containsExactly(winnerDevice);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM devices WHERE employee_uuid = ?", Integer.class, employee))
        .isEqualTo(1);
    // one OK audit row for the winner, one CONFLICT row for the loser, nothing else
    assertThat(
            jdbc.queryForList(
                "SELECT envelope->'payload'->>'result' FROM outbox_events "
                    + "WHERE envelope->'payload'->>'action' = 'DEVICE_REGISTER' "
                    + "AND envelope->'payload'->>'actor' = ?",
                String.class,
                employee.toString()))
        .containsExactlyInAnyOrder("OK", "CONFLICT");
  }

  @Test
  void should_revoke_existing_device_and_let_only_one_parallel_replacement_win() throws Exception {
    var employee = UUID.randomUUID();
    var initialBody =
        mvc.perform(
                post("/self-service/v1/devices")
                    .header(
                        "Authorization",
                        TestTokens.bearer(TestTokens.valid(employee, Role.EMPLOYEE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"publicKey\":\""
                            + new PhoneSimulator(employee).publicKeyBase64()
                            + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var initialDeviceId = json.readTree(initialBody).get("deviceId").asText();

    var responses = runParallelRegistrations(employee);

    assertThat(responses)
        .extracting(MockHttpServletResponse::getStatus)
        .containsExactlyInAnyOrder(201, 409);
    var winner = responses.stream().filter(r -> r.getStatus() == 201).findFirst().orElseThrow();
    var winnerDevice = json.readTree(winner.getContentAsString()).get("deviceId").asText();

    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM devices WHERE device_id = ?", String.class, initialDeviceId))
        .isEqualTo("REVOKED");
    assertThat(
            jdbc.queryForList(
                "SELECT device_id FROM devices WHERE employee_uuid = ? AND status = 'ACTIVE'",
                String.class,
                employee))
        .containsExactly(winnerDevice);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM devices WHERE employee_uuid = ?", Integer.class, employee))
        .isEqualTo(2);
  }
}
