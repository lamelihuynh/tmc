-- Edge node database. One database and one login per service (ADR-10); only the attendance service
-- uses this server today. Flyway creates the tables on first start.
CREATE ROLE attendance LOGIN PASSWORD 'attendance';
CREATE DATABASE attendance_edge OWNER attendance;
