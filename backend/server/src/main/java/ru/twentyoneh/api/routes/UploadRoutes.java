package ru.twentyoneh.api.routes;

import io.javalin.http.Context;
import ru.twentyoneh.api.error.BadRequest;
import ru.twentyoneh.service.FileService;

import java.util.Map;

public final class UploadRoutes {
    public static void upload(Context ctx, FileService fs) throws Exception {
        var uf = ctx.uploadedFile("file");
        if(uf == null) throw new BadRequest("field 'file' is required");
        try(var in = uf.content()){
            var res = fs.upload(uf.filename(), uf.size(), in);
            ctx.json(Map.of(
                    "id", res.id().toString(),
                    "token", res.token(),
                    "downloadUrl", res.downloadUrl(),
                    "originalName", res.originalName(),
                    "sizeBytes", res.sizeBytes()
            ));
        }
    }
}
