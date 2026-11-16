-- Edge node database. One database and one login per service (ADR-10): attendance here, gate-station
-- in 02-gate-store.sql. Flyway creates the tables on first start.
CREATE ROLE attendance LOGIN PASSWORD 'attendance';
CREATE DATABASE attendance_edge OWNER attendance;
