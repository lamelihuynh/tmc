-- Transactional outbox for AuditRecorded (C-08, ADR-04, ADR-15). Same table as attendance and as
-- platform/messaging JdbcOutboxStore (reference: platform/messaging/src/main/resources/tmc-messaging/outbox_events.sql).
CREATE TABLE outbox_events (
    seq            BIGSERIAL PRIMARY KEY,
    message_id     UUID        NOT NULL UNIQUE,
    event_type     VARCHAR(48) NOT NULL,
    envelope       JSONB       NOT NULL,
    publish_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    failure_count  INT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);
CREATE INDEX idx_outbox_pending ON outbox_events (publish_status, seq);
