-- Presence read model (D-04): one row per scan (AccessAttempt). aggregate_version orders versions of
-- the same scan only (a correction has a higher one); presence is the latest scanned_at per employee
-- and factory. Written in the same transaction as processed_messages (ADR-05).
CREATE TABLE attendance_projection (
    attempt_id        UUID         PRIMARY KEY,
    employee_uuid     UUID         NOT NULL,
    factory_code      VARCHAR(16)  NOT NULL,
    direction         VARCHAR(8)   NOT NULL,
    scanned_at        TIMESTAMPTZ  NOT NULL,
    aggregate_version BIGINT       NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX attendance_projection_latest
    ON attendance_projection (factory_code, employee_uuid, scanned_at DESC);

CREATE INDEX attendance_projection_employee
    ON attendance_projection (employee_uuid, scanned_at DESC);
