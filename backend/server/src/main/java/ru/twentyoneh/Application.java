package ru.twentyoneh;

import io.javalin.Javalin;
import ru.twentyoneh.api.error.ApiErrorHandler;
import ru.twentyoneh.api.error.BadRequest;
import ru.twentyoneh.api.routes.DownloadRoutes;
import ru.twentyoneh.api.routes.UploadRoutes;
import ru.twentyoneh.config.AppConfig;
import ru.twentyoneh.config.DataSourceFactory;
import ru.twentyoneh.config.LiquibaseRunner;
import ru.twentyoneh.dto.FileRecord;
import ru.twentyoneh.repository.FilesRepositoryJdbc;
import ru.twentyoneh.service.FileService;
import ru.twentyoneh.service.TokenService;
import ru.twentyoneh.storage.LocalStorage;
import ru.twentyoneh.storage.Storage;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Application {
    static final Logger logger = Logger.getLogger(ru.twentyoneh.Application.class.getName());

    public static void main(String[] args) throws IOException {
        var cfg = AppConfig.fromEnv();
        Files.createDirectories(cfg.storageDir());
        // подключение к БД
        var ds = DataSourceFactory.create(cfg);
        // миграции
        LiquibaseRunner.run(ds);

        // создание репозитория, логики локального хранилища
        var repo    = new FilesRepositoryJdbc(ds);
        Storage storage = new LocalStorage(cfg.storageDir(), cfg.maxUploadBytes());
        var tokens  = new TokenService();
        var files   = new FileService(repo, storage, tokens, cfg.baseUrl());

        // настройка сервера
        var app = Javalin.create(jc ->{
            jc.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                   it.defaultScheme = "http";
                   it.allowHost("localhost:5500");
                });
            });
            jc.http.defaultContentType = "application/json";
            jc.jetty.modifyServer(s -> s.setStopAtShutdown(true));
        });

        // проверка подключения к БД
        try (var conn = ds.getConnection();
             var st = conn.createStatement();
             var rs = st.executeQuery("select version()")){
            if (rs.next()) {
                logger.log(Level.INFO, "DB Version found: " + rs.getString(1));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "DB error: " + e.getMessage());
            throw new RuntimeException(e);
        }

        // обработчик ошибок
        ApiErrorHandler.install(app);

        // Эндроинты
        app.get("api/health", ctx -> ctx.json(Map.of("status","ok")));
        app.post("/api/files", ctx -> UploadRoutes.upload(ctx, files));
        app.get ("/d/{token}",  ctx -> DownloadRoutes.download(ctx, files));

        app.start(cfg.port());
    }
}