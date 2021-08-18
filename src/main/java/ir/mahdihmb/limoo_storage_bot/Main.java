package ir.mahdihmb.limoo_storage_bot;

import ir.limoo.driver.exception.LimooException;
import ir.mahdihmb.limoo_storage_bot.core.ConfigService;
import ir.mahdihmb.limoo_storage_bot.core.CoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final transient Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws LimooException {
        logger.info("--------------- Starting bot ---------------");
        CoreManager.initApp();

        String limooUrl = ConfigService.get("bot.limooUrl");
        String botUsername = ConfigService.get("bot.username");
        String botPassword = ConfigService.get("bot.password");
        new LimooStorageBot(limooUrl, botUsername, botPassword).run();

        logger.info("--------------- Bot started! ---------------");
    }
}
