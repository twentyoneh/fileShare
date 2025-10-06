package ru.twentyoneh.dto;

import java.time.Instant;
import java.util.UUID;

public record FileRecord(
        UUID id, String originalName, String storedKey, long sizeBytes,
        String mimeType, String sha256, String token,
        Instant createdAt, Instant lastDownloadAt, int downloadCount
) {}
