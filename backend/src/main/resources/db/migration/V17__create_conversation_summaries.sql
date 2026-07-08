-- summary_text stores AES-GCM ciphertext (see EncryptedStringConverter), sized with
-- the same ~2x inflation margin already established for mood_check_ins.note (V7).
CREATE TABLE conversation_summaries (
    id                            UUID PRIMARY KEY,
    user_id                       UUID NOT NULL UNIQUE REFERENCES users(id),
    summary_text                  VARCHAR(6000) NOT NULL,
    summarized_through_message_id UUID NOT NULL REFERENCES conversation_messages(id),
    updated_at                    TIMESTAMPTZ NOT NULL,
    version                       BIGINT NOT NULL DEFAULT 0
);
