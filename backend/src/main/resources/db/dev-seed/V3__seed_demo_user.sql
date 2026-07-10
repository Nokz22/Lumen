-- Synthetic demo user only, loaded when spring.flyway.locations includes db/dev-seed
-- (dev profile). Never real personal data — see docs/constitution.md "Dados 100% sintéticos".
INSERT INTO users (id, email, display_name, locale, region, created_at, version)
VALUES ('11111111-1111-1111-1111-111111111111', 'demo@lumen.dev', 'Demo User', 'en', 'PT', now(), 0);
