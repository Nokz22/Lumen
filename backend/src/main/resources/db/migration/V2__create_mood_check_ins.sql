CREATE TABLE mood_check_ins (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users(id),
    emotion        VARCHAR(20) NOT NULL,
    energy_level   INTEGER NOT NULL CHECK (energy_level BETWEEN 1 AND 5),
    sleep_hours    NUMERIC(4, 2) NOT NULL,
    sleep_quality  INTEGER NOT NULL CHECK (sleep_quality BETWEEN 1 AND 5),
    note           VARCHAR(1000),
    check_in_date  DATE NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    version        BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_mood_check_ins_user_date UNIQUE (user_id, check_in_date)
);

CREATE INDEX idx_mood_check_ins_user_id ON mood_check_ins (user_id);
