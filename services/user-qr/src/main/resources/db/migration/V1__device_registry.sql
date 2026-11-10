-- Device Key Registry (ADR-12). Public keys only; private keys never leave the phones.
-- Storage-level encryption at rest (AES-256) is a deployment concern, see docs/architecture/privacy-notes.md.
CREATE TABLE devices (
    device_id     VARCHAR(64) PRIMARY KEY,           -- also the JWS "kid"
    employee_uuid UUID        NOT NULL,
    public_key    TEXT        NOT NULL,              -- Base64 X.509 SubjectPublicKeyInfo (EC P-256)
    status        VARCHAR(16) NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX uq_one_active_device_per_employee ON devices (employee_uuid) WHERE status = 'ACTIVE';
CREATE INDEX idx_devices_updated ON devices (updated_at);
