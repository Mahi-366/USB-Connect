package com.usbair.connect.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Reads settings in this order (first one found wins):
 *   1. -Dkey=value on the command line          e.g. mvn test -Dheadless=true
 *   2. Environment variable (dots become _, upper case)  e.g. LOGIN_PASSWORD
 *   3. config.properties in the project root folder
 */
public final class Config {

    private static final Properties FILE_PROPS = new Properties();

    static {
        Path file = Path.of("config.properties");
        if (Files.exists(file)) {
            try (InputStream in = new FileInputStream(file.toFile())) {
                FILE_PROPS.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Could not read config.properties", e);
            }
        }
    }

    private Config() {
    }

    public static String get(String key) {
        String value = System.getProperty(key);
        if (isBlank(value)) {
            value = System.getenv(key.replace('.', '_').toUpperCase());
        }
        if (isBlank(value)) {
            value = FILE_PROPS.getProperty(key);
        }
        if (isBlank(value)) {
            throw new IllegalStateException("Missing setting '" + key + "'. "
                    + "Copy config.properties.example to config.properties and fill it in.");
        }
        return value.trim();
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
