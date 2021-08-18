package ir.mahdihmb.limoo_storage_bot.core;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

public class DataSourceManager {

    private static final String driverClassName = "org.postgresql.Driver";
    private static final String DB_URL_TEMPLATE = "jdbc:postgresql://%s:%s/%s";
    private static String dbUrl;
    private static DataSource dataSource;

    private DataSourceManager() {
    }

    protected static void init() {
        dbUrl = String.format(
                DB_URL_TEMPLATE,
                ConfigService.get("db.host"),
                ConfigService.get("db.port"),
                ConfigService.get("db.name")
        );
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(driverClassName);
        basicDataSource.setUrl(dbUrl);
        basicDataSource.setUsername(ConfigService.get("db.username"));
        basicDataSource.setPassword(ConfigService.get("db.password"));
        dataSource = basicDataSource;
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static String getDriverClassName() {
        return driverClassName;
    }

    public static String getDbUrl() {
        return dbUrl;
    }
}
