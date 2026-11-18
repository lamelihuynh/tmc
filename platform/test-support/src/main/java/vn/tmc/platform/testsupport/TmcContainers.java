package vn.tmc.platform.testsupport;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Containers for integration tests, with the same images as docker/docker-compose.yml. RabbitMQ
 * needs ~45 s to boot on an arm64 laptop, more than Testcontainers' default wait, hence the longer
 * startup timeouts. Use with {@code @Testcontainers} and {@code @Container static final}.
 */
public final class TmcContainers {
  private TmcContainers() {}

  public static final String POSTGRES_IMAGE = "postgres:16-alpine";
  public static final String RABBIT_IMAGE = "rabbitmq:3.13-management-alpine";
  public static final String REDIS_IMAGE = "redis:7-alpine";

  public static PostgreSQLContainer<?> postgres() {
    return new PostgreSQLContainer<>(POSTGRES_IMAGE).withStartupTimeout(Duration.ofMinutes(2));
  }

  public static RabbitMQContainer rabbit() {
    return new RabbitMQContainer(RABBIT_IMAGE).withStartupTimeout(Duration.ofMinutes(3));
  }

  public static GenericContainer<?> redis() {
    return new GenericContainer<>(REDIS_IMAGE)
        .withExposedPorts(6379)
        .waitingFor(Wait.forListeningPort())
        .withStartupTimeout(Duration.ofMinutes(2));
  }
}
