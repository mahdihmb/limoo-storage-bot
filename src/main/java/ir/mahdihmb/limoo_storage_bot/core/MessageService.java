package ir.mahdihmb.limoo_storage_bot.core;

import ir.mahdihmb.limoo_storage_bot.Main;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class MessageService {

    private static final String MESSAGES_FILE = "/i18n/messages_fa.properties";
    private static ResourceBundle resourceBundle;

    private MessageService() {
    }

    protected static void init() throws IOException {
        InputStream stream = Main.class.getResourceAsStream(MESSAGES_FILE);
        resourceBundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    public static String get(String key) {
        return resourceBundle.getString(key);
    }

    public static String get(String key, String defaultValue) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            return defaultValue;
        }
    }

}
