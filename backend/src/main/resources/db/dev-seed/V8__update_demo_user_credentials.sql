-- Real (but obviously non-production) credentials for manual testing: demo@lumen.dev /
-- Demo1234! — documented in README. Also grants the consent the demo user needs to be
-- able to check in at all, now that Phase 2 gates it.
UPDATE users
SET password_hash = '$2b$10$/RvvxBzFC1/8OChCkw2aiOw87yN4zclh4sTZW2eoYus2ZD6q8GYA6',
    role           = 'USER',
    date_of_birth  = '1990-01-01'
WHERE id = '11111111-1111-1111-1111-111111111111';

INSERT INTO consent_records (id, user_id, consent_type, granted, consent_version, granted_at, revoked_at, created_at)
VALUES (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'HEALTH_DATA_PROCESSING',
    true,
    1,
    now(),
    NULL,
    now()
);
