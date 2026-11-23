-- Seed the Gate Store so the "gate-store" profile starts with the same gates as application.yml.
-- Production data is maintained through /admin/v1/gates; this migration only fills an empty catalog.
INSERT INTO gates (gate_id, factory_code, hardware_gate_id, mode, maintenance)
VALUES
    ('BD-GATE-01', 'BD', 'BD-HW-01', 'ENTRY_ONLY', false),
    ('BD-GATE-02', 'BD', 'BD-HW-02', 'EXIT_ONLY', false),
    ('BD-GATE-03', 'BD', 'BD-HW-03', 'BIDIRECTIONAL', false),
    ('LA-GATE-01', 'LA', 'LA-HW-01', 'BIDIRECTIONAL', false)
ON CONFLICT DO NOTHING;
