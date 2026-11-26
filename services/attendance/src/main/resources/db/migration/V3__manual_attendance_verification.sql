-- B-07: retain HR verification evidence on the manual attempt, without changing its gate status.
-- Existing MANUAL rows remain unverified and must not count as attendance automatically.
ALTER TABLE access_attempts
    ADD COLUMN manual_verified_by UUID,
    ADD COLUMN manual_verification_reason VARCHAR(64),
    ADD COLUMN manual_verified_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_manual_verification CHECK (
        (manual_verified_by IS NULL AND manual_verification_reason IS NULL AND manual_verified_at IS NULL)
        OR
        (manual_verified_by IS NOT NULL AND manual_verification_reason IS NOT NULL
         AND manual_verified_at IS NOT NULL AND source = 'MANUAL' AND status = 'COMMITTED'
         AND employee_uuid IS NOT NULL AND direction IS NOT NULL AND anomaly IS NULL
         AND manual_verification_reason ~ '^[A-Z][A-Z0-9_]{1,63}$')
    );
