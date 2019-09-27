package at.mechatron.doorctrlservice;

import java.io.IOException;
import java.util.Properties;

public enum ApplicationProperties {
    INSTANCE;

    private final Properties properties;

    ApplicationProperties() {
        properties = new Properties();
    }

    public void load() throws IOException {
        properties.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
    }

    public String getBaseUrl() {
        return properties.getProperty("base-url");
    }

    public String getAuthorizationKey() {
        return properties.getProperty("authorization-key");
    }
}
