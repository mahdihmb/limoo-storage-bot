package ir.mahdihmb.limoo_storage_bot.core;

public class CoreManager {

    public static void initApp() {
        try {
            ConfigService.init();
            MessageService.init();
            DataSourceManager.init();
            HibernateSessionManager.init();
            FlywayManager.init();
            FlywayManager.migrate();
        } catch (Throwable tx) {
            throw new ExceptionInInitializerError(tx);
        }
    }

    public static void initFlywayOnly() {
        try {
            ConfigService.init();
            DataSourceManager.init();
            FlywayManager.init();
        } catch (Throwable tx) {
            throw new ExceptionInInitializerError(tx);
        }
    }

}
