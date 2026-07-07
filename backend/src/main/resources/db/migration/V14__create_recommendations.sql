-- UNIQUE(mood_check_in_id, exercise_id) is the database-level backstop for consumer
-- idempotency: even if the same MoodCheckInSubmittedEvent is processed twice (message
-- redelivery), the rule engine cannot insert the same exercise recommendation for the
-- same check-in a second time.
CREATE TABLE recommendations (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL REFERENCES users(id),
    mood_check_in_id  UUID NOT NULL REFERENCES mood_check_ins(id),
    exercise_id       UUID NOT NULL REFERENCES exercises(id),
    reason            VARCHAR(500) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_recommendations_check_in_exercise UNIQUE (mood_check_in_id, exercise_id)
);

CREATE INDEX idx_recommendations_user_id ON recommendations (user_id);
