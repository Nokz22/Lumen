CREATE TABLE audit_log_entries (
    id              UUID PRIMARY KEY,
    actor_user_id   UUID NOT NULL REFERENCES users(id),
    subject_user_id UUID NOT NULL REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_log_entries_subject_user_id ON audit_log_entries (subject_user_id);
