package ru.twentyoneh;

import io.javalin.Javalin;
import org.flywaydb.core.Flyway;
import ru.twentyoneh.api.error.ApiErrorHandler;
import ru.twentyoneh.api.error.BadRequest;
import ru.twentyoneh.config.AppConfig;
import ru.twentyoneh.config.DataSourceFactory;
import ru.twentyoneh.config.FlywayMaker;
import ru.twentyoneh.dto.FileRecord;
import ru.twentyoneh.repository.FilesRepositoryJdbc;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class Application {
    public static void main(String[] args) throws IOException {
        var cfg = AppConfig.fromEnv();
        Files.createDirectories(cfg.storageDir());

        // создание миграций
        var ds = DataSourceFactory.create(cfg);
        FlywayMaker.migrate(ds);

        var repo = new FilesRepositoryJdbc(ds);

        // настройка сервера
        var app = Javalin.create(jc ->{
            jc.http.defaultContentType = "application/json";
            jc.jetty.modifyServer(s -> s.setStopAtShutdown(true));
        });

        // проверка подключения к БД
        try (var conn = ds.getConnection();
             var st = conn.createStatement();
             var rs = st.executeQuery("select version()")){
            if (rs.next()) {
                System.out.println("DB version: " + rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
            throw new RuntimeException(e);
        }

        ApiErrorHandler.install(app);

        // Эндроинты
        app.get("api/health", ctx -> ctx.json(Map.of("status","ok")));
        app.get("/api/test400", ctx -> {
            throw new BadRequest("Bad Request");
        });


        //Тесты FilesRepo
        app.post("/api/dev/seed", ctx -> {
            var id = UUID.randomUUID();
            var token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(java.util.UUID.randomUUID().toString().getBytes());
            var rec = new FileRecord(
                    id, "hello.txt", "aa/bb/" + id + ".txt", 12L,
                    "text/plain", null, token,
                    java.time.Instant.now(), null, 0
            );
            repo.insert(rec);
            ctx.json(java.util.Map.of("id", id.toString(), "token", token));
        });

        // получить по токену
        app.get("/api/dev/by-token/{token}", ctx -> {
            var tok = ctx.pathParam("token");
            var opt = repo.findByToken(tok);
            if (opt.isEmpty()) { ctx.status(404).json(java.util.Map.of("error","not_found")); return; }
            var r = opt.get();
            ctx.json(java.util.Map.of("id", r.id().toString(), "name", r.originalName(), "size", r.sizeBytes()));
        });

        // счётчик и список
        app.get("/api/dev/count", ctx -> ctx.json(java.util.Map.of("count", repo.countAll())));
        app.get("/api/dev/list", ctx -> ctx.json(repo.list(10,0)));

        app.start(cfg.port());
    }
}