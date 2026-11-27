-- Append-only audit log (D-08, NFR-02, AC-05, ADR-16).
-- Flyway runs as the owner (auditadmin); the service runs as auditadmin_app, which only gets
-- INSERT and SELECT. A trigger rejects UPDATE, DELETE and TRUNCATE for every role, the owner
-- included. Each row stores hash = SHA-256(prev_hash || canonical entry), see AuditHashChain.
CREATE TABLE audit_entries (
    seq         BIGSERIAL    PRIMARY KEY,
    entry_id    UUID         NOT NULL UNIQUE,
    occurred_at TIMESTAMPTZ  NOT NULL,
    source      VARCHAR(64),
    action      VARCHAR(128) NOT NULL,
    actor       VARCHAR(128),
    target      VARCHAR(256),
    details     JSONB        NOT NULL DEFAULT '{}'::jsonb,
    prev_hash   CHAR(64)     NOT NULL UNIQUE, -- two entries can never chain onto the same one
    hash        CHAR(64)     NOT NULL UNIQUE,
    stored_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX audit_entries_occurred_at ON audit_entries (occurred_at DESC);
CREATE INDEX audit_entries_action ON audit_entries (action, occurred_at DESC);
CREATE INDEX audit_entries_actor ON audit_entries (actor, occurred_at DESC);

CREATE FUNCTION audit_entries_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_entries is append-only: % is not allowed', TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_entries_no_update_delete
    BEFORE UPDATE OR DELETE ON audit_entries
    FOR EACH ROW EXECUTE FUNCTION audit_entries_append_only();

CREATE TRIGGER audit_entries_no_truncate
    BEFORE TRUNCATE ON audit_entries
    FOR EACH STATEMENT EXECUTE FUNCTION audit_entries_append_only();

-- The restricted login is created by docker/init/central/01-databases.sql. Where it does not exist
-- (Testcontainers, an old Docker volume) the grants are skipped and the owner login keeps working.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'auditadmin_app') THEN
        REVOKE ALL ON audit_entries FROM auditadmin_app;
        GRANT SELECT, INSERT ON audit_entries TO auditadmin_app;
        GRANT USAGE ON SEQUENCE audit_entries_seq_seq TO auditadmin_app;
        GRANT SELECT, INSERT ON processed_messages TO auditadmin_app;
    END IF;
END
$$;
