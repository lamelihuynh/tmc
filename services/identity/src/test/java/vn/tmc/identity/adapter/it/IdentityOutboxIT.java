package vn.tmc.identity.adapter.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.identity.application.port.in.ILogin;
import vn.tmc.identity.bootstrap.IdentityApplication;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.messaging.InMemoryEventBroker;
import vn.tmc.platform.messaging.jdbc.JdbcOutboxStore;
import vn.tmc.platform.messaging.outbox.OutboxRelay;
import vn.tmc.platform.messaging.outbox.OutboxStore;
import vn.tmc.platform.testsupport.TmcContainers;

/**
 * C-11: identity on the default profile keeps its audit outbox in PostgreSQL, so an event that was
 * not published yet survives a restart of the service and is published afterwards. The scheduled
 * relay is off; the test runs the relay itself against an in-memory broker (the AMQP leg is covered
 * by MessagingInfraIT). Needs Docker: ./gradlew :services:identity:integrationTest
 */
@Tag("integration")
@Testcontainers
class IdentityOutboxIT {
  @Container static final PostgreSQLContainer<?> POSTGRES = TmcContainers.postgres();

  static final String NV001 = "0192a1c0-0000-7000-8000-000000000001";

  private static ConfigurableApplicationContext startIdentity() {
    // command-line arguments, so they win over application.yml (builder properties would not)
    return new SpringApplicationBuilder(IdentityApplication.class)
        .run(
            "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
            "--spring.datasource.username=" + POSTGRES.getUsername(),
            "--spring.datasource.password=" + POSTGRES.getPassword(),
            "--server.port=0",
            "--tmc.outbox.relay-enabled=false");
  }

  private static String status(JdbcTemplate jdbc, UUID messageId) {
    return jdbc.queryForObject(
        "SELECT publish_status FROM outbox_events WHERE message_id = ?", String.class, messageId);
  }

  @Test
  void should_keep_login_audit_pending_across_a_restart_and_publish_it_afterwards() {
    UUID succeeded;
    UUID failed;
    try (var identity = startIdentity()) {
      assertThat(identity.getBean(OutboxStore.class)).isInstanceOf(JdbcOutboxStore.class);
      var login = identity.getBean(ILogin.class);
      login.login("nv001", "dev-only-change-me", null);
      login.login("nv001", "wrong", null);

      var jdbc = identity.getBean(JdbcTemplate.class);
      succeeded = messageId(jdbc, "LOGIN_SUCCEEDED");
      failed = messageId(jdbc, "LOGIN_FAILED");
      assertThat(status(jdbc, succeeded)).isEqualTo("PENDING");
      assertThat(status(jdbc, failed)).isEqualTo("PENDING");
    } // identity stops before the relay ever ran

    try (var identity = startIdentity()) {
      var jdbc = identity.getBean(JdbcTemplate.class);
      var outbox = identity.getBean(OutboxStore.class);
      assertThat(status(jdbc, succeeded)).isEqualTo("PENDING");
      assertThat(outbox.fetchPending(1000))
          .extracting(EventEnvelope::messageId)
          .containsSubsequence(succeeded, failed);

      var broker = new InMemoryEventBroker();
      new OutboxRelay(outbox, broker, 100).runOnce();

      assertThat(broker.published())
          .extracting(EventEnvelope::messageId)
          .containsSubsequence(succeeded, failed);
      assertThat(broker.published())
          .filteredOn(e -> e.messageId().equals(failed))
          .singleElement()
          .satisfies(
              e -> {
                assertThat(e.source()).isEqualTo("identity");
                assertThat(e.payload())
                    .containsEntry("target", NV001)
                    .containsEntry("result", "BAD_PASSWORD");
              });
      assertThat(status(jdbc, succeeded)).isEqualTo("PUBLISHED");
      assertThat(status(jdbc, failed)).isEqualTo("PUBLISHED");
    }
  }

  private static UUID messageId(JdbcTemplate jdbc, String action) {
    return jdbc.queryForObject(
        "SELECT message_id FROM outbox_events WHERE envelope->'payload'->>'action' = ? "
            + "AND envelope->'payload'->>'target' = ? ORDER BY seq DESC LIMIT 1",
        UUID.class,
        action,
        NV001);
  }
}
