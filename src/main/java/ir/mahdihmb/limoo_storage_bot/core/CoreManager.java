package ir.mahdihmb.limoo_storage_bot.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreManager {

    private static final transient Logger logger = LoggerFactory.getLogger(CoreManager.class);

    public static void initApp() {
        try {
            ConfigService.init();
            MessageService.init();
            DataSourceManager.init();
            HibernateSessionManager.init();
            FlywayManager.init();
            FlywayManager.migrate();
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }

    public static void initFlywayOnly() {
        try {
            ConfigService.init();
            DataSourceManager.init();
            FlywayManager.init();
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }

    public static void reInitDatabaseInRuntime() {
        try {
            logger.info("------- Reinitializing database -------");
            DataSourceManager.init();
            HibernateSessionManager.init();
            logger.info("------- Database reinitialized! -------");
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }
}
