-- The note column now stores AES-GCM ciphertext (nonce + tag + base64 overhead), which
-- inflates a 1000-character plaintext note beyond its original column size.
ALTER TABLE mood_check_ins ALTER COLUMN note TYPE VARCHAR(2000);
