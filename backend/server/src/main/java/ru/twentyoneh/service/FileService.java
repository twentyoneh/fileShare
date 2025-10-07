package ru.twentyoneh.service;

import ru.twentyoneh.repository.FilesRepository;

public final class FileService {
    private final FilesRepository repo;
    private final Storage storage;
    private final TokenService tokens;
    private final String baseUrl;
}
