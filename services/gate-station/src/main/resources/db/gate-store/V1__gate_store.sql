-- Gate Store (ADR-14): gate-station's own database on the edge node (ADR-10 database per service).
-- gates replaces the seed in application.yml once A-04 adds JdbcGateConfigRepository.

CREATE TABLE gates (
    gate_id          VARCHAR(64)  PRIMARY KEY,
    factory_code     VARCHAR(16)  NOT NULL,
    hardware_gate_id VARCHAR(64)  NOT NULL UNIQUE,   -- id in the Turnstile Control System
    mode             VARCHAR(16)  NOT NULL CHECK (mode IN ('ENTRY_ONLY', 'EXIT_ONLY', 'BIDIRECTIONAL')),
    maintenance      BOOLEAN      NOT NULL DEFAULT false,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_gates_factory ON gates (factory_code);

-- Outbox for gate audit events (manual open and its reason), routing key gate-station.AuditRecorded
-- (ADR-15). Same table as platform/messaging JdbcOutboxStore.
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
