-- Ordered guided-session instructions, one row per step. Populated for every
-- exercise category except BREATHING, which is guided by the phase timings on
-- `exercises` instead (see V18).
CREATE TABLE exercise_steps (
    exercise_id UUID NOT NULL REFERENCES exercises(id),
    step_order  INTEGER NOT NULL,
    instruction VARCHAR(500) NOT NULL,
    PRIMARY KEY (exercise_id, step_order)
);

-- WALKING
INSERT INTO exercise_steps (exercise_id, step_order, instruction) VALUES
    ('33333333-3333-3333-3333-333333333303', 0, 'Step outside and take a moment to notice your surroundings.'),
    ('33333333-3333-3333-3333-333333333303', 1, 'Walk at a comfortable pace, letting your thoughts settle as you move.'),
    ('33333333-3333-3333-3333-333333333303', 2, 'Notice three things you can see, hear or feel along the way.'),
    ('33333333-3333-3333-3333-333333333303', 3, 'Head back slowly, and take one deep breath before you go back inside.'),
    ('33333333-3333-3333-3333-333333333304', 0, 'Start walking at a slightly faster pace than usual.'),
    ('33333333-3333-3333-3333-333333333304', 1, 'Let your arms swing naturally as you move.'),
    ('33333333-3333-3333-3333-333333333304', 2, 'Bring your attention to your breathing as you keep the pace up.'),
    ('33333333-3333-3333-3333-333333333304', 3, 'Ease off the pace for the last stretch to cool down.');

-- STRETCHING
INSERT INTO exercise_steps (exercise_id, step_order, instruction) VALUES
    ('33333333-3333-3333-3333-333333333305', 0, 'Slowly tilt your head toward one shoulder and hold.'),
    ('33333333-3333-3333-3333-333333333305', 1, 'Tilt your head toward the other shoulder and hold.'),
    ('33333333-3333-3333-3333-333333333305', 2, 'Roll your shoulders backward a few times, slowly.'),
    ('33333333-3333-3333-3333-333333333305', 3, 'Roll your shoulders forward a few times, slowly.'),
    ('33333333-3333-3333-3333-333333333305', 4, 'Let your arms hang loose and relax for a moment.'),
    ('33333333-3333-3333-3333-333333333306', 0, 'Reach both arms overhead and stretch tall.'),
    ('33333333-3333-3333-3333-333333333306', 1, 'Fold forward gently, letting your head and arms hang.'),
    ('33333333-3333-3333-3333-333333333306', 2, 'Twist gently to one side, then the other.'),
    ('33333333-3333-3333-3333-333333333306', 3, 'Stretch each leg out in front of you, one at a time.'),
    ('33333333-3333-3333-3333-333333333306', 4, 'Finish standing tall, taking a few slow breaths.');

-- SLEEP_HYGIENE
INSERT INTO exercise_steps (exercise_id, step_order, instruction) VALUES
    ('33333333-3333-3333-3333-333333333307', 0, 'Dim the lights in the room around you.'),
    ('33333333-3333-3333-3333-333333333307', 1, 'Put screens away for the rest of this session.'),
    ('33333333-3333-3333-3333-333333333307', 2, 'Do something calming — reading, gentle stretching or quiet music.'),
    ('33333333-3333-3333-3333-333333333307', 3, 'Prepare your space for sleep before the time is up.'),
    ('33333333-3333-3333-3333-333333333308', 0, 'Put your phone and other screens out of reach.'),
    ('33333333-3333-3333-3333-333333333308', 1, 'Choose a screen-free activity you enjoy.'),
    ('33333333-3333-3333-3333-333333333308', 2, 'Notice how your mind feels without the constant input.'),
    ('33333333-3333-3333-3333-333333333308', 3, 'Keep screens away until the hour is up.');

-- BEHAVIORAL_ACTIVATION
INSERT INTO exercise_steps (exercise_id, step_order, instruction) VALUES
    ('33333333-3333-3333-3333-333333333309', 0, 'Pick one small task you have been putting off.'),
    ('33333333-3333-3333-3333-333333333309', 1, 'Do just that one thing — nothing more is expected of you right now.'),
    ('33333333-3333-3333-3333-333333333309', 2, 'Notice how it feels to have completed it.'),
    ('33333333-3333-3333-3333-333333333310', 0, 'Think of one person you would like to hear from.'),
    ('33333333-3333-3333-3333-333333333310', 1, 'Send them a short message or make a quick call.'),
    ('33333333-3333-3333-3333-333333333310', 2, 'Notice how the contact feels, whatever their response.');

-- GROUNDING
INSERT INTO exercise_steps (exercise_id, step_order, instruction) VALUES
    ('33333333-3333-3333-3333-333333333311', 0, 'Name 5 things you can see around you.'),
    ('33333333-3333-3333-3333-333333333311', 1, 'Name 4 things you can touch.'),
    ('33333333-3333-3333-3333-333333333311', 2, 'Name 3 things you can hear.'),
    ('33333333-3333-3333-3333-333333333311', 3, 'Name 2 things you can smell.'),
    ('33333333-3333-3333-3333-333333333311', 4, 'Name 1 thing you can taste.'),
    ('33333333-3333-3333-3333-333333333312', 0, 'Place both feet flat on the ground.'),
    ('33333333-3333-3333-3333-333333333312', 1, 'Notice the contact between your feet and the floor.'),
    ('33333333-3333-3333-3333-333333333312', 2, 'Press down gently and feel the support beneath you.'),
    ('33333333-3333-3333-3333-333333333312', 3, 'Take one slow breath before you go on with your day.');
