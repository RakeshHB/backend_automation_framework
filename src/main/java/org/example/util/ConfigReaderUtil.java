package org.example.util;

import lombok.extern.log4j.Log4j2;
import org.example.exception.ValidationException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Log4j2
public final class ConfigReaderUtil {

    private static final Properties properties;
    private static final String CONFIG_FILE_PATH = "config.properties";

    // Private constructor to prevent instantiation
    private ConfigReaderUtil() {}

    static {
        properties = new Properties();
        loadProperties();
    }

    private static void loadProperties() {
        try (FileInputStream fileInputStream = new FileInputStream(CONFIG_FILE_PATH)) {
            properties.load(fileInputStream);
        } catch (IOException e) {
            log.error("Unable to load properties file: " + CONFIG_FILE_PATH);
            throw new RuntimeException(e);
        }
    }

    public static String getConfig(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            String msg = String.format("property %s not found in %s", key, CONFIG_FILE_PATH);
            log.error(msg);
            throw new ValidationException(msg);
        }
        return value;
    }

}
