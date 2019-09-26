package at.mechatron.doorctrlservice;

import at.mechatron.doorctrlservice.doorctrl.DoorControlClient;
import at.mechatron.doorctrlservice.doorctrl.ModbusDoorControlClient;
import at.mechatron.doorctrlservice.facerecognition.FaceRecognitionService;
import at.mechatron.doorctrlservice.facerecognition.PollingHttpFaceRecognitionService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class Service {
    private static final Logger LOG = LogManager.getLogger();

    public static void main(final String args[]) throws IOException {
        final String idClass = args[1];
        final String relayIp = args[2];

        final Properties prop = new Properties();
        prop.load(new FileInputStream("config.properties"));

        final String baseUrl = prop.getProperty("base-url");
        final String authorizationKey = prop.getProperty("authorization-key");

        LOG.info("Starting service with config");
        LOG.info("Event ID Class: {}", idClass);
        LOG.info("Relay IP Address: {}", relayIp);
        LOG.info("Base URL: {}", baseUrl);
        LOG.info("Authorization Key: {}", authorizationKey.length() > 0 ? "<Not empty>" : "<Empty>");

        final DoorControlClient doorControlClient = new ModbusDoorControlClient(relayIp);
        final FaceRecognitionService faceRecognitionService = new PollingHttpFaceRecognitionService(
                baseUrl,
                authorizationKey,
                Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                        .setDaemon(false)
                        .setNameFormat("TODO")
                        .setUncaughtExceptionHandler((thread, throwable) -> LOG.fatal("Face Recognition thread terminated", throwable))
                        .build()));

        final WatchDog watchDog = new WatchDog(doorControlClient, faceRecognitionService);
        watchDog.watch();
    }
}
