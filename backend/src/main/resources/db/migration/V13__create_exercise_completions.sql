CREATE TABLE exercise_completions (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL REFERENCES users(id),
    exercise_id       UUID NOT NULL REFERENCES exercises(id),
    recommendation_id UUID,
    completed_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_exercise_completions_user_id ON exercise_completions (user_id);
