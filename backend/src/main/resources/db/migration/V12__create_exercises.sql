-- Exercise library is reference content the app needs to function, not per-user data,
-- so it is seeded here (like crisis_resources in V11) rather than in db/dev-seed.
CREATE TABLE exercises (
    id               UUID PRIMARY KEY,
    category         VARCHAR(30) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    intensity        VARCHAR(10) NOT NULL,
    rationale        VARCHAR(1000) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_exercises_category ON exercises (category);

INSERT INTO exercises (id, category, name, duration_minutes, intensity, rationale, created_at) VALUES
    ('33333333-3333-3333-3333-333333333301', 'BREATHING', '4-7-8 Breathing', 5, 'LOW',
        'Slowing your exhale activates the parasympathetic nervous system, which can ease a racing mind and body.', now()),
    ('33333333-3333-3333-3333-333333333302', 'BREATHING', 'Box Breathing', 5, 'LOW',
        'A steady four-count rhythm gives your attention somewhere calm to land when thoughts feel scattered.', now()),
    ('33333333-3333-3333-3333-333333333303', 'WALKING', '10-Minute Outdoor Walk', 10, 'LOW',
        'Light movement and a change of scenery can shift both energy and mood more than staying still.', now()),
    ('33333333-3333-3333-3333-333333333304', 'WALKING', 'Brisk Walk Around the Block', 15, 'MEDIUM',
        'A slightly faster pace raises your heart rate gently, which is linked to improved mood in the short term.', now()),
    ('33333333-3333-3333-3333-333333333305', 'STRETCHING', 'Neck and Shoulder Release', 5, 'LOW',
        'Tension often collects in the neck and shoulders without us noticing — a few slow stretches can loosen it.', now()),
    ('33333333-3333-3333-3333-333333333306', 'STRETCHING', 'Full-Body Stretch Sequence', 10, 'LOW',
        'Moving through a full stretch sequence gives your body a chance to release tension built up over the day.', now()),
    ('33333333-3333-3333-3333-333333333307', 'SLEEP_HYGIENE', 'Wind-Down Routine', 15, 'LOW',
        'A consistent pre-sleep routine signals to your body that it is time to slow down, which can make falling asleep easier.', now()),
    ('33333333-3333-3333-3333-333333333308', 'SLEEP_HYGIENE', 'Screen-Free Hour', 60, 'LOW',
        'Reducing screen light before bed supports your body''s natural melatonin production.', now()),
    ('33333333-3333-3333-3333-333333333309', 'BEHAVIORAL_ACTIVATION', 'One Small Task', 10, 'LOW',
        'Completing even one small task can create a sense of momentum when everything feels like too much.', now()),
    ('33333333-3333-3333-3333-333333333310', 'BEHAVIORAL_ACTIVATION', 'Reach Out to Someone', 10, 'LOW',
        'Brief social contact, even a short message, can interrupt a low mood more than waiting it out alone.', now()),
    ('33333333-3333-3333-3333-333333333311', 'GROUNDING', '5-4-3-2-1 Senses', 5, 'LOW',
        'Naming what you can see, hear and touch brings attention back to the present moment when things feel overwhelming.', now()),
    ('33333333-3333-3333-3333-333333333312', 'GROUNDING', 'Feet on the Floor', 3, 'LOW',
        'Focusing on physical contact with the ground is a fast way to interrupt spiraling thoughts.', now());
