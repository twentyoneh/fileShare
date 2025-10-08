package ru.twentyoneh.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Arrays;

public final class FlywayMaker {
    private static final Logger log = LoggerFactory.getLogger(FlywayMaker.class);
    public static void migrate(DataSource ds) {
        var conf = Flyway.configure()
                .dataSource(ds)
                .failOnMissingLocations(true)
                .locations("classpath:db/migration")
                .schemas("public")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(false)
                .outOfOrder(true);

        log.info("flyway.locations (conf): {}", Arrays.toString(conf.getLocations()));

        conf.load().migrate();
    }
}
