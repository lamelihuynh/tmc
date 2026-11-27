-- Central site databases: one per service, one login per service, no cross access (ADR-10).
-- user_qr and identity (outbox, C-11) are used by code today; the others are reserved so teams can
-- add Flyway migrations.
CREATE ROLE userqr LOGIN PASSWORD 'userqr';
CREATE DATABASE user_qr OWNER userqr;

CREATE ROLE reporting LOGIN PASSWORD 'reporting';
CREATE DATABASE reporting OWNER reporting;

CREATE ROLE hrintegration LOGIN PASSWORD 'hrintegration';
CREATE DATABASE hr_integration OWNER hrintegration;

CREATE ROLE auditadmin LOGIN PASSWORD 'auditadmin';
CREATE DATABASE audit_admin OWNER auditadmin;
-- D-08: audit-admin runs as this login, which its Flyway V2 allows only INSERT and SELECT; the
-- owner above is used by Flyway alone. An existing volume needs `make nuke && make up` to get it.
CREATE ROLE auditadmin_app LOGIN PASSWORD 'auditadmin_app';

CREATE ROLE identity LOGIN PASSWORD 'identity';
CREATE DATABASE identity OWNER identity;
