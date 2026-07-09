CREATE TABLE conversation_messages (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id),
    role       VARCHAR(20) NOT NULL,
    content    VARCHAR(8000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_conversation_messages_user_id_created_at ON conversation_messages (user_id, created_at);
