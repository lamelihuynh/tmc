-- Fraud alerts of ADR-12 R4 (D-05), for HR review (UC-09). The flagged pass stays untouched in
-- attendance_projection; one alert per pass and rule.
CREATE TABLE fraud_alerts (
    attempt_id    UUID         NOT NULL,
    rule          VARCHAR(32)  NOT NULL,
    employee_uuid UUID         NOT NULL,
    factory_code  VARCHAR(16)  NOT NULL,
    other_factory VARCHAR(16)  NOT NULL,
    detail        TEXT         NOT NULL,
    occurred_at   TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (attempt_id, rule)
);

CREATE INDEX fraud_alerts_factory ON fraud_alerts (factory_code, occurred_at DESC);
CREATE INDEX fraud_alerts_other_factory ON fraud_alerts (other_factory, occurred_at DESC);
