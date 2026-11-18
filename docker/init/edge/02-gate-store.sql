-- Gate Station's own database on the edge node (ADR-10, ADR-14): gate catalog (A-04) and the
-- outbox of gate audit events. Flyway (classpath:db/gate-store) creates the tables when
-- gate-station starts with the "gate-store" profile.
-- Containers created before this file existed do not run it: `make nuke && make up`, or run the
-- two statements below by hand (docs/runbook/troubleshooting.md).
CREATE ROLE gatestation LOGIN PASSWORD 'gatestation';
CREATE DATABASE gate_store OWNER gatestation;
