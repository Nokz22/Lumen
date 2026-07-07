CREATE TABLE assessments (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id),
    assessment_type VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_assessments_user_id_type ON assessments (user_id, assessment_type);

CREATE TABLE assessment_responses (
    id            UUID PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES assessments(id),
    item_number   INTEGER NOT NULL,
    value         INTEGER NOT NULL CHECK (value BETWEEN 0 AND 3),
    CONSTRAINT uq_assessment_responses_item UNIQUE (assessment_id, item_number)
);

CREATE INDEX idx_assessment_responses_assessment_id ON assessment_responses (assessment_id);

CREATE TABLE assessment_scores (
    id             UUID PRIMARY KEY,
    assessment_id  UUID NOT NULL UNIQUE REFERENCES assessments(id),
    total_score    INTEGER NOT NULL,
    wellbeing_band VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL
);
