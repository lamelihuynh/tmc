-- Consumer dedup (ADR-05), table of platform/messaging JdbcProcessedMessageStore
-- (reference DDL: platform/messaging/src/main/resources/tmc-messaging/processed_messages.sql).
-- Owned by this service only (ADR-10 database per service).
CREATE TABLE processed_messages (
    consumer     VARCHAR(64) NOT NULL,
    message_id   UUID        NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, message_id)
);
