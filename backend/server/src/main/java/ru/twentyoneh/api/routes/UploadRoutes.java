package ru.twentyoneh.api.routes;

import io.javalin.http.Context;
import ru.twentyoneh.api.error.BadRequest;
import ru.twentyoneh.dto.struct.UploadResult;
import ru.twentyoneh.service.FileService;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UploadRoutes {

    public static void upload(Context ctx, FileService fileService) throws Exception {
        var uf = ctx.uploadedFile("file"); // input type="file" из html
        if(uf.size() == 0 || uf.filename().isEmpty()) throw new BadRequest(uf.filename() + "ошибка чтения файла");

        try(var inputStream = uf.content()){
            UploadResult res = fileService.upload(uf.filename(), uf.size(), inputStream); // попытка записи файла
            ctx.json(Map.of(
                    "id", res.getId().toString(),
                    "token", res.getToken(),
                    "downloadUrl", res.getDownloadUrl(),
                    "originalName", res.getOriginalName(),
                    "sizeBytes", res.getSizeBytes()
            ));
        }
    }
}
