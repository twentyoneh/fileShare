package ru.twentyoneh.api.routes;

import io.javalin.http.Context;
import ru.twentyoneh.service.FileService;

public class DownloadRoutes {
    public static void download(Context ctx, FileService fs) throws Exception {
        var token = ctx.pathParam("token");
        var dl = fs.openByToken(token);
        ctx.header("Content-Disposition", FileService.contentDisposition(dl.getRecord().getOriginalName()));
        ctx.header("Cache-Control", "no-store");
        ctx.contentType("application/octet-stream");
        ctx.result(dl.getStream());
    }
}
