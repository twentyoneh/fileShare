package ru.twentyoneh.config;

import java.nio.file.Path;

public record AppConfig(
        int port,
        String baseUrl,
        String dbUrl, String dbUser, String dbPassword,
        Path storageDir,
        long maxUploadBytes,
        int retentionDays
) {
    public static AppConfig fromEnv() {
        return new AppConfig(
                Integer.parseInt(get("PORT", "8080")),
                get("BASE_URL", "http://localhost:8080"),
                get("DB_URL", "jdbc:postgresql://localhost:5432/filesvc"),
                get("DB_USER", "filesvc"),
                get("DB_PASSWORD", "filesvc"),
                Path.of(get("STORAGE_DIR", "./storage")),
                Long.parseLong(get("MAX_UPLOAD_MB", "256")) * 1024 * 1024L,
                Integer.parseInt(get("RETENTION_DAYS", "30"))
        );
    }
    private static String get(String k, String def){ var v=System.getenv(k); return v==null?def:v; }
}
