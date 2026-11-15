-- HR corrections of counted passes (story B-06, AsyncAPI AttendanceCorrected).
-- attendance_transactions.attempt_id stays the primary key that references access_attempts, so a
-- correction can never be a second transaction row (that would double count). Instead each
-- correction is its own row pointing at the original; the original row is never updated, which
-- keeps the history auditable (NFR-02).

CREATE TABLE attendance_corrections (
    correction_id        UUID         PRIMARY KEY,
    original_attempt_id  UUID         NOT NULL REFERENCES attendance_transactions (attempt_id),
    revision             INT          NOT NULL CHECK (revision >= 1),
    employee_uuid        UUID         NOT NULL,
    direction            VARCHAR(8)   NOT NULL,   -- value after the correction
    occurred_at          TIMESTAMPTZ  NOT NULL,   -- value after the correction
    previous_direction   VARCHAR(8)   NOT NULL,
    previous_occurred_at TIMESTAMPTZ  NOT NULL,
    reason               VARCHAR(64)  NOT NULL,   -- a code, never free text (ADR-09)
    corrected_by         UUID         NOT NULL,   -- internal UUID of the HR user
    source               VARCHAR(16)  NOT NULL,   -- source of the original pass
    corrected_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_correction_revision UNIQUE (original_attempt_id, revision)
);
CREATE INDEX idx_corrections_employee_time ON attendance_corrections (employee_uuid, occurred_at);
