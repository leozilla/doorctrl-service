package at.mechatron.doorctrlservice;

import at.mechatron.doorctrlservice.doorctrl.DoorControlClient;
import at.mechatron.doorctrlservice.doorctrl.ModbusDoorControlClient;
import at.mechatron.doorctrlservice.facerecognition.FaceRecognitionService;
import at.mechatron.doorctrlservice.facerecognition.PollingFaceRecognitionService;
import at.mechatron.doorctrlservice.facerecognition.safrapi.SAFRHttpClient;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Service {
    private static final Logger LOG = LogManager.getLogger(Service.class);

    public static void main(final String args[]) throws IOException {
        LOG.info("Program args {}", Arrays.asList(args));

        if (args.length != 2) {
            LOG.fatal("Usage: doorctrl-service [idClass] [relayIPAddress]");
            System.exit(10);
        }

        final String idClass = args[1];
        final String relayIp = args[2];

        ApplicationProperties.INSTANCE.load();

        final String baseUrl = ApplicationProperties.INSTANCE.getBaseUrl();
        final String authorizationKey = ApplicationProperties.INSTANCE.getAuthorizationKey();
        final Duration doorLockDuration = ApplicationProperties.INSTANCE.getDoorLockDuration().orElse(Duration.ofMinutes(2));
        final Duration httpPollInterval = ApplicationProperties.INSTANCE.getHttpPoolInterval().orElse(Duration.ofMillis(1500));

        LOG.info("Event ID Class: {}", idClass);
        LOG.info("Relay IP Address: {}", relayIp);
        LOG.info("Base URL: {}", baseUrl);
        LOG.info("Door lock duration: {}", doorLockDuration);
        LOG.info("HTTP poll interval: {}", doorLockDuration);

        if (baseUrl.isEmpty()) {
            LOG.fatal("Base URL is empty");
            System.exit(10);
        }

        if (authorizationKey.isEmpty()) {
            LOG.fatal("Authorization key is empty");
            System.exit(11);
        }

        final Thread.UncaughtExceptionHandler handler = (thread, throwable) -> LOG.fatal("Thread terminated", throwable);
        final Executor eventLoop = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(false)
                .setNameFormat("EventLoop")
                .setUncaughtExceptionHandler(handler)
                .build());
        final Executor modbusIOExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Modbus Client Blocking IO")
                .setUncaughtExceptionHandler(handler)
                .build());
        final Executor safrHttpIOExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("SAFR HTTP Client Blocking IO")
                .setUncaughtExceptionHandler(handler)
                .build());
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(false)
                .setNameFormat("Scheduler")
                .setUncaughtExceptionHandler(handler)
                .build());

        final DoorControlClient doorControlClient = new ModbusDoorControlClient(relayIp, modbusIOExecutor);
        final FaceRecognitionService faceRecognitionService = new PollingFaceRecognitionService(
                httpPollInterval,
                new SAFRHttpClient(baseUrl, authorizationKey, httpPollInterval, safrHttpIOExecutor),
                scheduler,
                eventLoop);

        final WatchDog watchDog = new WatchDog(doorLockDuration, doorControlClient, faceRecognitionService, scheduler, eventLoop);
        watchDog.watch();
    }
}
