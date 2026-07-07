CREATE TABLE wearable_readings (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL REFERENCES users(id),
    type         VARCHAR(30) NOT NULL,
    value        NUMERIC(10, 4) NOT NULL,
    recorded_at  TIMESTAMPTZ NOT NULL,
    source       VARCHAR(30) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

-- Matches the access pattern of WearableInsightService: fetch a user's readings of a
-- given type within a date range, ordered by time.
CREATE INDEX idx_wearable_readings_user_type_recorded ON wearable_readings (user_id, type, recorded_at);
