package ru.twentyoneh;

import io.javalin.Javalin;
import org.flywaydb.core.Flyway;
import ru.twentyoneh.api.error.ApiErrorHandler;
import ru.twentyoneh.api.error.BadRequest;
import ru.twentyoneh.config.AppConfig;
import ru.twentyoneh.config.DataSourceFactory;
import ru.twentyoneh.config.FlywayMaker;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class Application {
    public static void main(String[] args) throws IOException {
        var cfg = AppConfig.fromEnv();
        Files.createDirectories(cfg.storageDir());

        var ds = DataSourceFactory.create(cfg);
        FlywayMaker.migrate(ds);

        var app = Javalin.create(jc ->{
            jc.http.defaultContentType = "application/json";
            jc.jetty.modifyServer(s -> s.setStopAtShutdown(true));
        });

        ApiErrorHandler.install(app);

        app.get("/api/test400", ctx -> {
            throw new BadRequest("Bad Request");
        });
        app.get("api/health", ctx -> ctx.json(Map.of("status","ok")));

        app.start(cfg.port());
    }
}