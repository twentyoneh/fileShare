package ru.twentyoneh.repository;

import ru.twentyoneh.dto.FileRecord;

import java.util.List;
import java.util.Optional;

public interface FilesRepository {
    void insert(FileRecord rec);
    Optional<FileRecord> findByToken(String token);
    long countAll();
    List<FileRecord> list(int limit, int offset);
}
