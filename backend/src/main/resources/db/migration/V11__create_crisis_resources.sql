-- CrisisResource is public reference data (helplines), not per-user data, so it is
-- seeded here rather than in db/dev-seed and exists in every environment.
--
-- IMPORTANT: verify these contacts and hours are current before any public demo or
-- production use — numbers and availability windows for support lines change, and
-- showing a wrong one in a crisis context is unacceptable (project-brief.md §6.4).
-- SNS 24 and 112 are stable, well-known national numbers; the SOS Voz Amiga contact
-- below should be double-checked against their current published details.
CREATE TABLE crisis_resources (
    id           UUID PRIMARY KEY,
    region       VARCHAR(10) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(30) NOT NULL,
    contact      VARCHAR(255) NOT NULL,
    availability VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_crisis_resources_region ON crisis_resources (region);

INSERT INTO crisis_resources (id, region, name, type, contact, availability, created_at) VALUES
    ('22222222-2222-2222-2222-222222222221', 'PT', 'SNS 24', 'HELPLINE', '808 24 24 24', '24/7', now()),
    ('22222222-2222-2222-2222-222222222222', 'PT', 'SOS Voz Amiga', 'HELPLINE', '213 544 545 / 912 802 669 / 963 524 660', '16h-24h', now()),
    ('22222222-2222-2222-2222-222222222223', 'PT', 'Emergência (112)', 'EMERGENCY_SERVICE', '112', '24/7', now());
