package ru.twentyoneh.service;

import ru.twentyoneh.api.error.BadRequest;
import ru.twentyoneh.api.error.NotFound;
import ru.twentyoneh.dto.FileRecord;
import ru.twentyoneh.dto.struct.Download;
import ru.twentyoneh.dto.struct.UploadResult;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FileService {
    private final FilesRepository repo;
    private final Storage storage;
    private final TokenService tokens;
    private final String baseUrl;
    private final Logger logger = Logger.getLogger(FileService.class.getName());



    public FileService(FilesRepository repo, Storage storage, TokenService tokens, String baseUrl) {
        this.repo = Objects.requireNonNull(repo);
        this.storage = Objects.requireNonNull(storage);
        this.tokens = Objects.requireNonNull(tokens);
        this.baseUrl = Objects.requireNonNull(baseUrl);
    }

    public UploadResult upload(String originalName, long size, InputStream body) throws IOException {
        if(originalName == null || originalName.isBlank()) throw new BadRequest("filename is required");
        if(size <= 0) throw new BadRequest("filesize is required");

        logger.log(Level.INFO, "Start {0} file", originalName);
        String ext = safeExt(originalName);
        if(ext == "") throw new BadRequest("file extension error");
        String storedKey = storage.store(body, size, ext);

        String token = tokens.newToken();
        UUID id = UUID.randomUUID();
        var rec = new FileRecord(
                id, originalName, storedKey, size,
                ext, null, token, Instant.now(), null, 0
        );
        repo.insert(rec);
        logger.log(Level.INFO, "Store {0} file complete!\n Token for search: {1}\n id: {2}", new Object[]{originalName, token, id});

        return new UploadResult(id, token, baseUrl + "/d/" + token, originalName, size);

    }

    public Download openByToken(String token) throws Exception {
        var rec = repo.findByToken(token).orElseThrow(NotFound::new);
        var in = storage.open(rec.getStoredKey());
        repo.incDownloadAndTouch(rec.getId());
        return new Download(rec, in);
    }

    public static String contentDisposition(String filename){
        String ascii = filename.replace("\"","'");
        String utf8 = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + utf8;
    }


    private static String safeExt(String name){
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length()-1) throw new BadRequest("file extension error"); // -1 если точки нет, или нет расширения
        if(i == 0) return name;
        String ext = name.substring(i).toLowerCase(Locale.ROOT); //
        return ext.matches("\\.[a-z0-9]{1,8}") ? ext : "";
    }
}
