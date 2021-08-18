package ir.mahdihmb.limoo_storage_bot.core;

import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ConfigService {

    private static final transient Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private static final String CONFIG_PROPERTIES_FILE = "config.properties";
    private static Configuration configuration;

    private ConfigService() {
    }

    protected static void init() {
        PropertiesConfiguration defaultPropConfig = new PropertiesConfiguration();
        FileHandler defaultConfigFileHandler = new FileHandler(defaultPropConfig);
        defaultConfigFileHandler.setFileName(CONFIG_PROPERTIES_FILE);
        Configuration defaultConfig;
        try {
            defaultConfigFileHandler.load();
            defaultConfig = defaultPropConfig.interpolatedConfiguration();
        } catch (ConfigurationException e) {
            logger.error("Can't get config properties from " + CONFIG_PROPERTIES_FILE, e);
            return;
        }

        String envName = defaultConfig.getString("config.environmentVariableName");
        String userDefinedConfigPath = System.getenv().get(envName);
        Configuration userDefinedConfig = null;
        if (userDefinedConfigPath != null) {
            PropertiesConfiguration userDefinedPropConfig = new PropertiesConfiguration();
            FileHandler userDefinedConfigFileHandler = new FileHandler(userDefinedPropConfig);
            userDefinedConfigFileHandler.setFile(new File(userDefinedConfigPath));
            try {
                userDefinedConfigFileHandler.load();
                userDefinedConfig = userDefinedPropConfig.interpolatedConfiguration();
            } catch (ConfigurationException e) {
                logger.error("Can't get user defined config properties from " + userDefinedConfigPath, e);
            }
        }

        CombinedConfiguration config = new CombinedConfiguration(new OverrideCombiner());
        if (userDefinedConfig != null)
            config.addConfiguration(userDefinedConfig);
        config.addConfiguration(defaultConfig);
        configuration = config;
    }

    public static String get(String key) {
        return configuration.getString(key);
    }

    public static String get(String key, String defaultValue) {
        return configuration.getString(key, defaultValue);
    }

}
