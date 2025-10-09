package ru.twentyoneh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class FileRecord{
    UUID id;
    String originalName;
    String storedKey;
    long sizeBytes;
    String mimeType;
    String sha256;
    String token;
    Instant createdAt;
    Instant lastDownloadAt;
    int downloadCount;
}
