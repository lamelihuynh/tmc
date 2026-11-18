package vn.tmc.hrintegration.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.InMemoryProcessedMessageStore;
import vn.tmc.platform.messaging.ProcessedMessageStore;
import vn.tmc.platform.messaging.jdbc.JdbcProcessedMessageStore;

/**
 * Consumer dedup store (ADR-05): table processed_messages (Flyway V1) in the default profile,
 * process memory on the Docker-free inmem and local profiles. The marker and the handler run in one
 * transaction, so a failed handler rolls the marker back and the redelivery is processed again.
 * TODO Team D: move the read model to the same database so it shares that transaction.
 */
@Configuration
class DedupStoreConfig {

  @Configuration
  @Profile({"inmem", "local"})
  static class InMemory {
    @Bean
    ProcessedMessageStore processedMessageStore() {
      return new InMemoryProcessedMessageStore();
    }

    @Bean
    IdempotentConsumer.TransactionRunner consumerTransaction() {
      return IdempotentConsumer.TransactionRunner.none();
    }
  }

  @Configuration
  @Profile("!inmem & !local")
  static class Jdbc {
    @Bean
    ProcessedMessageStore processedMessageStore(JdbcTemplate jdbc) {
      return new JdbcProcessedMessageStore(jdbc);
    }

    @Bean
    IdempotentConsumer.TransactionRunner consumerTransaction(PlatformTransactionManager tm) {
      var template = new TransactionTemplate(tm);
      return work -> Boolean.TRUE.equals(template.execute(status -> work.getAsBoolean()));
    }
  }
}
