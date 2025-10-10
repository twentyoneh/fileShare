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
    List<FileRecord> selectExpired(int retentionDays, int limit);

    void incDownloadAndTouch(UUID id);
    void deleteById(UUID id);
}
