-- Transactional outbox table (ADR-04). Copy into the producing service's own Flyway migration.
-- Same text as JdbcOutboxStore.DDL and the same shape as attendance V1.
CREATE TABLE IF NOT EXISTS outbox_events (
    seq            BIGSERIAL PRIMARY KEY,
    message_id     UUID        NOT NULL UNIQUE,
    event_type     VARCHAR(48) NOT NULL,
    envelope       JSONB       NOT NULL,
    publish_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    failure_count  INT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox_events (publish_status, seq);
