-- Sync log towards HR/Payroll (document.md 5.9): one row per attendance attempt. Keeps the line that
-- was sent so the reconcile job can look it up and resend it after a restart, and so the deadline
-- alert of the 1st (D-07) still counts unsynced rows after a restart.
CREATE TABLE sync_log (
    event_id          UUID         PRIMARY KEY,
    status            VARCHAR(16)  NOT NULL,
    attempts          INT          NOT NULL,
    last_error        TEXT,
    updated_at        TIMESTAMPTZ  NOT NULL,
    aggregate_version BIGINT       NOT NULL,
    batch_id          UUID         NOT NULL,
    attendance_period CHAR(7),
    employee_uuid     UUID,
    factory_code      VARCHAR(16),
    direction         VARCHAR(8),
    scanned_at        TIMESTAMPTZ,
    corrects_event_id UUID,
    next_attempt_at   TIMESTAMPTZ
);

CREATE INDEX sync_log_retry ON sync_log (status, next_attempt_at);
CREATE INDEX sync_log_period ON sync_log (attendance_period, status);
