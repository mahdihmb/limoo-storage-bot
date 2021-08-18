package ir.mahdihmb.limoo_storage_bot.core;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

public class FlywayManager {

    protected static final String MIGRATIONS_LOCATION = "migrations";
    private static Flyway flyway;

    private FlywayManager() {
    }

    protected static void init() {
        DataSource dataSource = DataSourceManager.getDataSource();
        flyway = Flyway.configure()
                .locations(MIGRATIONS_LOCATION)
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .dataSource(dataSource)
                .load();
    }

    public static void clean() {
        flyway.clean();
    }

    public static void migrate() {
        flyway.migrate();
    }
}
