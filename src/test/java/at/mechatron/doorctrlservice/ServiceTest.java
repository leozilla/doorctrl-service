package at.mechatron.doorctrlservice;

import at.mechatron.doorctrlservice.doorctrl.DoorControlClient;
import at.mechatron.doorctrlservice.facerecognition.FaceRecognitionService;
import at.mechatron.doorctrlservice.facerecognition.PollingFaceRecognitionService;
import at.mechatron.doorctrlservice.facerecognition.safrapi.FaceRecognitionEvent;
import at.mechatron.doorctrlservice.facerecognition.safrapi.SAFRClient;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ServiceTest {
    private static final Logger LOG = LogManager.getLogger();

    private static class FakeDoorControlClient implements DoorControlClient {
        private AtomicBoolean isLocked = new AtomicBoolean();

        public boolean isLocked() {
            return isLocked.get();
        }

        @Override
        public CompletableFuture<Void> lockDoor() {
            isLocked.set(true);
            return null;
        }

        @Override
        public CompletableFuture<Void> unlockDoor() {
            isLocked.set(false);
            return null;
        }
    }

    private static class FakeSAFRClient implements SAFRClient {
        public List<FaceRecognitionEvent> events;

        @Override
        public CompletableFuture<List<FaceRecognitionEvent>> getEvents(final Instant startTime) {
            return CompletableFuture.completedFuture(
                    events.stream()
                            .sorted(Comparator.comparing(e -> e.getStartTime()))
                            .filter(e -> e.getStartTime() > startTime.toEpochMilli())
                            .collect(Collectors.toList()));
        }
    }

    private static FakeSAFRClient safrHttpClient;
    private static WatchDog watchDog;
    private static FakeDoorControlClient doorControlClient;

    @BeforeAll
    public static void beforeAll() {
        safrHttpClient = new FakeSAFRClient();
        doorControlClient = new FakeDoorControlClient();

        final Thread.UncaughtExceptionHandler handler = (thread, throwable) -> LOG.fatal("Thread terminated", throwable);
        final Executor eventLoop = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(false)
                .setNameFormat("EventLoop")
                .setUncaughtExceptionHandler(handler)
                .build());
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(false)
                .setNameFormat("Scheduler")
                .setUncaughtExceptionHandler(handler)
                .build());

        final FaceRecognitionService faceRecognitionService = new PollingFaceRecognitionService(
                Duration.ofMillis(500),
                safrHttpClient,
                scheduler,
                eventLoop);

        watchDog = new WatchDog(Duration.ofSeconds(5), doorControlClient, faceRecognitionService, scheduler, eventLoop);
    }

    @Test
    public void personInViewImmediatlyLockDoor() {
        Instant now = Instant.now().minusSeconds(1);
        safrHttpClient.events = Arrays.asList(createEvent("1", now.toEpochMilli(), 0));

        watchDog.watch();

        await().atMost(5, SECONDS).until(() -> doorControlClient.isLocked());
    }

    @Test
    public void todo() {
        watchDog.watch();
    }

    private FaceRecognitionEvent createEvent(final String personId, final long startTime, final long endTime) {
        return new FaceRecognitionEvent("any id", personId, startTime, endTime, "any id class");
    }
}
