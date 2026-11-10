-- Attendance edge schema (ADR-03 system of record, ADR-04 outbox, ADR-05 idempotency, ADR-11 state machine).
-- Owned exclusively by the attendance service (ADR-10 database per service).

CREATE TABLE access_attempts (
    attempt_id    UUID PRIMARY KEY,                -- = event_id = Idempotency-Key end to end
    employee_uuid UUID,
    gate_id       VARCHAR(64)  NOT NULL,
    factory_code  VARCHAR(16)  NOT NULL,
    direction     VARCHAR(8),
    jti           VARCHAR(64),                     -- token id only, never the raw token (ADR-09)
    source        VARCHAR(16)  NOT NULL,
    scanned_at    TIMESTAMPTZ  NOT NULL,
    status        VARCHAR(24)  NOT NULL,
    reason_code   VARCHAR(48),
    anomaly       VARCHAR(32),
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL
);
CREATE INDEX idx_attempts_status_updated ON access_attempts (status, updated_at);
CREATE INDEX idx_attempts_jti ON access_attempts (jti) WHERE jti IS NOT NULL;

CREATE TABLE attendance_transactions (
    attempt_id    UUID PRIMARY KEY REFERENCES access_attempts (attempt_id), -- unique: replay cannot double count
    employee_uuid UUID         NOT NULL,
    factory_code  VARCHAR(16)  NOT NULL,
    gate_id       VARCHAR(64)  NOT NULL,
    direction     VARCHAR(8)   NOT NULL,
    occurred_at   TIMESTAMPTZ  NOT NULL,
    source        VARCHAR(16)  NOT NULL
);
CREATE INDEX idx_tx_employee_time ON attendance_transactions (employee_uuid, occurred_at);

CREATE TABLE outbox_events (
    seq            BIGSERIAL PRIMARY KEY,          -- insertion order = publish order
    message_id     UUID        NOT NULL UNIQUE,
    event_type     VARCHAR(48) NOT NULL,
    envelope       JSONB       NOT NULL,
    publish_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',  -- PENDING | PUBLISHED (not an attempt state)
    failure_count  INT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);
CREATE INDEX idx_outbox_pending ON outbox_events (publish_status, seq);
