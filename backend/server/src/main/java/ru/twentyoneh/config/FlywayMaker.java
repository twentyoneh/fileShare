package ru.twentyoneh.config;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

public final class FlywayMaker {
    public static void migrate(DataSource ds) {
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .schemas("public")
                .baselineOnMigrate(false)
//                .baselineVersion("1")
                .load()
                .migrate();
    }
}
