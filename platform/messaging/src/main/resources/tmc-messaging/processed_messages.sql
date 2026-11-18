-- Consumer dedup table (ADR-05). Copy into the consuming service's own Flyway migration.
-- Same text as JdbcProcessedMessageStore.DDL.
CREATE TABLE IF NOT EXISTS processed_messages (
    consumer     VARCHAR(64) NOT NULL,
    message_id   UUID        NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, message_id)
);
