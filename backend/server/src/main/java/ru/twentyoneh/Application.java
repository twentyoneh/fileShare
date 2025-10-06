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
import java.sql.SQLException;
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

        app.get("/api/test400", ctx -> {
            throw new BadRequest("Bad Request");
        });
        app.get("api/health", ctx -> ctx.json(Map.of("status","ok")));

        app.start(cfg.port());
    }
}