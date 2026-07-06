CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    display_name  VARCHAR(255) NOT NULL,
    locale        VARCHAR(10) NOT NULL,
    region        VARCHAR(10) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0
);

-- Append-only: a new consent decision inserts a new row (granted/revoked history),
-- it never updates a previous row in place. No optimistic-locking version needed.
CREATE TABLE consent_records (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users(id),
    consent_type  VARCHAR(50) NOT NULL,
    granted       BOOLEAN NOT NULL,
    consent_version INT NOT NULL,
    granted_at    TIMESTAMPTZ,
    revoked_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_consent_records_user_id ON consent_records (user_id);
