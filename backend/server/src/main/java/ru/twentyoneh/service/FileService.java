package ru.twentyoneh.service;

import ru.twentyoneh.api.error.BadRequest;
import ru.twentyoneh.api.error.NotFound;
import ru.twentyoneh.dto.FileRecord;
import ru.twentyoneh.repository.FilesRepository;
import ru.twentyoneh.storage.Storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class FileService {
    private final FilesRepository repo;
    private final Storage storage;
    private final TokenService tokens;
    private final String baseUrl;

    // функции для создания объектов которые будут выступать в качестве возвращаемых значений, +- структуры, но можно описать прямо тут
    public record UploadResult(UUID id, String token, String downloadUrl, String originalName, long sizeBytes) {}
    public record Download(FileRecord record, InputStream stream) {}

    public FileService(FilesRepository repo, Storage storage, TokenService tokens, String baseUrl) {
        this.repo = Objects.requireNonNull(repo);
        this.storage = Objects.requireNonNull(storage);
        this.tokens = Objects.requireNonNull(tokens);
        this.baseUrl = Objects.requireNonNull(baseUrl);
    }

    public UploadResult upload(String originalName, long size, InputStream body) throws IOException {
        if(originalName == null || originalName.isBlank()) throw new BadRequest("filename is required");
        if(size <= 0) throw new BadRequest("filesize is required");

        String ext = safeExt(originalName);
        String storedKey = storage.store(body, size, ext);

        String token = tokens.newToken();
        UUID id = UUID.randomUUID();
        var rec = new FileRecord(
                id, originalName, storedKey, size,
                null, null, token, Instant.now(), null, 0
        );
        repo.insert(rec);

        return new UploadResult(id, token, baseUrl + "/d/" + token, originalName, size);

    }

    public Download openByToken(String token) throws Exception {
        var rec = repo.findByToken(token).orElseThrow(NotFound::new);
        var in = storage.open(rec.storedKey());
        repo.incDownloadAndTouch(rec.id());
        return new Download(rec, in);
    }

    public static String contentDisposition(String filename){
        String ascii = filename.replace("\"","'");
        String utf8 = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + utf8;
    }


    private static String safeExt(String name){
        int i = name.lastIndexOf('.');
        if (i <= 0 || i == name.length()-1) return "";
        String ext = name.substring(i).toLowerCase(Locale.ROOT);
        return ext.matches("\\.[a-z0-9]{1,8}") ? ext : "";
    }
}
