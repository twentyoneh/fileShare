package ru.twentyoneh.config;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;

public final class LiquibaseRunner {
    private static final Logger log = LoggerFactory.getLogger(LiquibaseRunner.class);
    private static final String MASTER = "db/changelog/db.changelog-master.yaml";

    public static void run(DataSource ds) {
        try (Connection conn = ds.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
            Liquibase liquibase = new Liquibase(MASTER, new ClassLoaderResourceAccessor(), database);

            log.info("Liquibase: applying {}", MASTER);
            liquibase.update(new Contexts(), new LabelExpression()); // можно передавать контексты/лейблы
            log.info("Liquibase: done");
        } catch (Exception e) {
            throw new RuntimeException("Liquibase migration failed", e);
        }
    }
}