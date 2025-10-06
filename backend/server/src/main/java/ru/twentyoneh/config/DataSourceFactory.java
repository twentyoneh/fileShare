package ru.twentyoneh.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSourceFactory {
    public static DataSource create(AppConfig c) {
        var hc = new HikariConfig();
        hc.setJdbcUrl(c.dbUrl());
        hc.setUsername(c.dbUser());
        hc.setPassword(c.dbPassword());
        hc.setMaximumPoolSize(10);
        hc.setInitializationFailTimeout(-1);
        return new HikariDataSource(hc);
    }
}
