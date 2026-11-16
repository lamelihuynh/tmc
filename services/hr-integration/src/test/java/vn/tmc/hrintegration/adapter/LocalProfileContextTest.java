package vn.tmc.hrintegration.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import vn.tmc.hrintegration.bootstrap.HrIntegrationApplication;
import vn.tmc.platform.messaging.InMemoryProcessedMessageStore;
import vn.tmc.platform.messaging.ProcessedMessageStore;

/** The Docker-free "local" profile starts without PostgreSQL or RabbitMQ. */
@SpringBootTest(classes = HrIntegrationApplication.class)
@ActiveProfiles("local")
class LocalProfileContextTest {
  @Autowired ApplicationContext context;
  @Autowired ProcessedMessageStore store;

  @Test
  void should_start_without_database_or_broker() {
    assertThat(store).isInstanceOf(InMemoryProcessedMessageStore.class);
    assertThat(
            context.getBeanNamesForType(org.springframework.amqp.rabbit.core.RabbitTemplate.class))
        .isEmpty();
  }
}
