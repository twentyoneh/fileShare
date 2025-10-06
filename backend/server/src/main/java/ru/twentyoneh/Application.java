package ru.twentyoneh;

import io.javalin.Javalin;
import ru.twentyoneh.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class Application {
    public static void main(String[] args) throws IOException {
        var cfg = AppConfig.fromEnv();
        Files.createDirectories(cfg.storageDir());

        var app = Javalin.create(jc ->{
            jc.http.defaultContentType = "application/json";
            jc.jetty.modifyServer(s -> s.setStopAtShutdown(true));
        });

        app.get("api/health", ctx -> ctx.json(Map.of("status","ok")));

        app.start(cfg.port());
    }
}