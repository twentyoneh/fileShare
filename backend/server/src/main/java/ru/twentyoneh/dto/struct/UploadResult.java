package ru.twentyoneh.dto.struct;

import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UploadResult {
    UUID id;
    String token;
    String downloadUrl;
    String originalName;
    long sizeBytes;
}
