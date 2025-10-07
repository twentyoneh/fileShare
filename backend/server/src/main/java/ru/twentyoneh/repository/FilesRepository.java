package ru.twentyoneh.repository;

import ru.twentyoneh.dto.FileRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FilesRepository {
    void insert(FileRecord rec);
    Optional<FileRecord> findByToken(String token);
    long countAll();
    List<FileRecord> list(int limit, int offset);

    void incDownloadAndTouch(UUID id);
    List<FileRecord> selectExpired(int retentionDays, int limit);
    void deleteById(UUID id);
}
