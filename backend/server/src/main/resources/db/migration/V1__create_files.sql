CREATE TABLE IF NOT EXISTS files (
                                     id               UUID PRIMARY KEY,
                                     owner_id         UUID,
                                     original_name    TEXT NOT NULL,
                                     stored_key       TEXT NOT NULL,
                                     size_bytes       BIGINT NOT NULL,
                                     mime_type        TEXT,
                                     sha256           TEXT,
                                     token            VARCHAR(64) NOT NULL UNIQUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_download_at TIMESTAMPTZ,
    download_count   INT NOT NULL DEFAULT 0
    );

CREATE INDEX IF NOT EXISTS idx_files_created_at       ON files(created_at);
CREATE INDEX IF NOT EXISTS idx_files_last_download_at ON files(last_download_at);
