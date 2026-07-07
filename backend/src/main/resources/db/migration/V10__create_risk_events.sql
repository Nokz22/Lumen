CREATE TABLE risk_events (
    id                     UUID PRIMARY KEY,
    user_id                UUID NOT NULL REFERENCES users(id),
    assessment_id          UUID REFERENCES assessments(id),
    trigger_source         VARCHAR(30) NOT NULL,
    status                 VARCHAR(30) NOT NULL,
    detected_at            TIMESTAMPTZ NOT NULL,
    resources_presented_at TIMESTAMPTZ,
    acknowledged_at        TIMESTAMPTZ,
    version                BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_risk_events_user_id ON risk_events (user_id);
