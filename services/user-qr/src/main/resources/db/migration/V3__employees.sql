-- Employee eligibility directory (C-02, ADR-09). Eligibility only: no name or other PII.
-- Inactive employees are kept with status = 'INACTIVE' so the edge receives departures via sync-bundle.
CREATE TABLE employees (
    employee_uuid UUID        PRIMARY KEY,
    status        VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    factories     TEXT[]      NOT NULL DEFAULT '{}',
    updated_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_employees_updated ON employees (updated_at);
