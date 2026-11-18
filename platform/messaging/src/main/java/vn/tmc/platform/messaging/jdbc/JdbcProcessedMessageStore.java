package vn.tmc.platform.messaging.jdbc;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.tmc.platform.messaging.ProcessedMessageStore;

/**
 * Consumer dedup in PostgreSQL (ADR-05, document.md 12.7): one row per (consumer, messageId). The
 * primary key makes {@link #markProcessed} atomic even with several consumer instances. To be
 * exactly-once in effect, call it in the same local transaction as the handler's writes ({@link
 * vn.tmc.platform.messaging.IdempotentConsumer} with a transaction runner). The owning service
 * creates the table with {@link #DDL} in its own Flyway migration.
 */
public class JdbcProcessedMessageStore implements ProcessedMessageStore {
  public static final String DDL =
      """
      CREATE TABLE IF NOT EXISTS processed_messages (
          consumer     VARCHAR(64) NOT NULL,
          message_id   UUID        NOT NULL,
          processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
          PRIMARY KEY (consumer, message_id)
      );
      """;

  private final JdbcTemplate jdbc;

  public JdbcProcessedMessageStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean markProcessed(String consumerName, UUID messageId) {
    int inserted =
        jdbc.update(
            "INSERT INTO processed_messages (consumer, message_id) VALUES (?, ?) "
                + "ON CONFLICT DO NOTHING",
            consumerName,
            messageId);
    return inserted == 1;
  }
}
