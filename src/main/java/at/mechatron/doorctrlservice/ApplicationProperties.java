package at.mechatron.doorctrlservice;

import com.google.common.base.Strings;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
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

    public String getEventsSource() {
        return properties.getProperty("events-source-filter");
    }

    public String getRelayIpAddress() {
        return properties.getProperty("relay-IP");
    }

    public String getAuthorizationKey() {
        return properties.getProperty("authorization-key");
    }

    public Optional<Duration> getDoorLockDuration() {
        String property = properties.getProperty("door-lock-duration-sec");
        return Strings.isNullOrEmpty(property)
                ? Optional.empty()
                : Optional.of(Duration.ofSeconds(Integer.parseInt(property)));
    }

    public Optional<Duration> getHttpPoolInterval() {
        String property = properties.getProperty("http-poll-interval-millis");
        return Strings.isNullOrEmpty(property)
                ? Optional.empty()
                : Optional.of(Duration.ofSeconds(Integer.parseInt(property)));
    }
}
