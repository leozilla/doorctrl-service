package at.mechatron.doorctrlservice;

import at.mechatron.doorctrlservice.doorctrl.DoorControlClient;
import at.mechatron.doorctrlservice.facerecognition.FaceRecognitionService;
import at.mechatron.doorctrlservice.facerecognition.PollingFaceRecognitionService;
import at.mechatron.doorctrlservice.facerecognition.safrapi.dto.FaceRecognitionEvent;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ServiceTest {

    private static class FakeDoorControlClient implements DoorControlClient {
        private AtomicBoolean isLocked = new AtomicBoolean();

        public boolean isLocked() {
            return isLocked.get();
        }

        @Override
        public CompletableFuture<Void> lockDoor() {
            isLocked.set(true);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> unlockDoor() {
            isLocked.set(false);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class FakeSAFRClient implements SAFRClient {
        public List<FaceRecognitionEvent> events;

        @Override
        public CompletableFuture<List<FaceRecognitionEvent>> getEvents(final String source, final Instant sinceTime) {
            return CompletableFuture.completedFuture(
                    events.stream()
                            .filter(e -> e.getStartTime() < Instant.now().toEpochMilli())
                            .sorted(Comparator.comparing(e -> e.getStartTime()))
                            .map(e -> hideEndTimeIfInFuture(e))
                            .filter(e -> e.getEndTime() == 0 || e.getEndTime() > sinceTime.toEpochMilli())
                            .collect(Collectors.toList()));
        }

        private FaceRecognitionEvent hideEndTimeIfInFuture(FaceRecognitionEvent e) {
            return e.getEndTime() > Instant.now().toEpochMilli()
                    ? new FaceRecognitionEvent(e.getEventId(), e.getSource(), e.getPersonId(), e.getStartTime(), 0, e.getIdClass())
                    : e;
        }
    }

    private static final Logger LOG = LogManager.getLogger(ServiceTest.class);

    private static final Duration DOOR_LOCK_DURATION = Duration.ofSeconds(3);
    private static final int WAIT_TOLERANCE_SEC = 2;

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
                "any camera",
                Duration.ofMillis(500),
                safrHttpClient,
                scheduler,
                eventLoop);

        watchDog = new WatchDog(DOOR_LOCK_DURATION, doorControlClient, faceRecognitionService, scheduler, eventLoop);
    }

    @Test
    public void personInView_shallImmediatelyLockDoor() {
        Instant now = Instant.now();
        safrHttpClient.events = Arrays.asList(createEvent("1", now.toEpochMilli(), 0));

        watchDog.watch();

        await().atMost(3, SECONDS).until(() -> doorControlClient.isLocked());
    }

    @Test
    public void personOutOfView_shallKeepDoorLocked() throws InterruptedException {
        Instant now = Instant.now();
        int inViewDurationMillis = 1500;
        safrHttpClient.events = Arrays.asList(createEvent("1", now.toEpochMilli(), now.plusMillis(inViewDurationMillis).toEpochMilli()));

        watchDog.watch();

        await().atMost(3, SECONDS).until(() -> doorControlClient.isLocked());
        Thread.sleep(inViewDurationMillis); // wait for the time the person is in the view
        Thread.sleep(DOOR_LOCK_DURATION.minusSeconds(1).toMillis()); // wait for almost the door lock duration
        assertThat(doorControlClient.isLocked(), equalTo(true));
    }

    @Test
    public void personOutOfViewForLongerThanDoorLockDuration_shallUnlockDoor() throws InterruptedException {
        Instant now = Instant.now();
        int inViewDurationMillis = 1500;
        safrHttpClient.events = Arrays.asList(createEvent("1", now.toEpochMilli(), now.plusMillis(inViewDurationMillis).toEpochMilli()));

        watchDog.watch();

        await().atMost(3, SECONDS).until(() -> doorControlClient.isLocked());
        Thread.sleep(inViewDurationMillis); // wait for the time the person is in the view
        assertThat(doorControlClient.isLocked(), equalTo(true));
        await().atMost(DOOR_LOCK_DURATION.getSeconds() + WAIT_TOLERANCE_SEC, SECONDS).until(() -> !doorControlClient.isLocked());
    }

    @Test
    public void personComesBackIntoViewBeforeDoorLockDurationElapsed_shallKeepDoorLocked() throws InterruptedException {
        Instant now = Instant.now();
        int inViewDurationMillis = 1500;
        safrHttpClient.events = Arrays.asList(
                createEvent("1", now.toEpochMilli(), now.plusMillis(inViewDurationMillis).toEpochMilli()),
                createEvent("1", now.plusMillis(inViewDurationMillis).plusSeconds(1).toEpochMilli(), 0));

        watchDog.watch();

        await().atMost(3, SECONDS).until(() -> doorControlClient.isLocked());
        Thread.sleep(inViewDurationMillis); // wait for the time the person is in the view
        Thread.sleep(DOOR_LOCK_DURATION.plusSeconds(2).toMillis()); // wait for longer than door lock duration
        assertThat(doorControlClient.isLocked(), equalTo(true));
    }

    @Test
    public void twoPersonsInView_oneLeaves_shallKeepDoorLocked() throws InterruptedException {
        Instant now = Instant.now();
        int inViewDurationMillis = 1500;
        safrHttpClient.events = Arrays.asList(
                createEvent("1", now.toEpochMilli(), now.plusMillis(inViewDurationMillis).toEpochMilli()), // person 1 leaves
                createEvent("2", now.toEpochMilli(), 0)); // person 2 stays

        watchDog.watch();

        await().atMost(3, SECONDS).until(() -> doorControlClient.isLocked());
        Thread.sleep(inViewDurationMillis); // wait for the time the person is in the view
        Thread.sleep(DOOR_LOCK_DURATION.plusSeconds(2).toMillis()); // wait for longer than door lock duration
        assertThat(doorControlClient.isLocked(), equalTo(true));
    }

    @Test
    public void twoPersonsInView_bothLeave_shallUnlockDoorAfterDoorLockDuration() throws InterruptedException {
        Instant now = Instant.now();
        int inViewDurationMillis = 3000;
        safrHttpClient.events = Arrays.asList(
                createEvent("1", now.toEpochMilli(), now.plusSeconds(2).toEpochMilli()), // person 1 leaves after 2 sec
                createEvent("2", now.toEpochMilli(), now.plusMillis(inViewDurationMillis).toEpochMilli())); // person 2 leaves as well

        watchDog.watch();

        await().atMost(3, SECONDS).until(() -> doorControlClient.isLocked());
        Thread.sleep(inViewDurationMillis); // wait for the time until both person leave the view
        assertThat(doorControlClient.isLocked(), equalTo(true));
        await().atMost(DOOR_LOCK_DURATION.getSeconds() + WAIT_TOLERANCE_SEC, SECONDS).until(() -> !doorControlClient.isLocked());
    }

    @Test
    public void twoPersonsInView_bothLeaveButOneComesBack_shallNotUnlockDoorAfterDoorLockDuration() throws InterruptedException {
        Instant now = Instant.now();
        safrHttpClient.events = Arrays.asList(
                createEvent("1", now.toEpochMilli(), now.plusSeconds(2).toEpochMilli()), // person 1 leaves after 2 sec
                createEvent("2", now.toEpochMilli(), now.plusSeconds(3).toEpochMilli()), // person 2 leaves as well after 3
                createEvent("1", now.plusSeconds(4).toEpochMilli(), 0)); // but person 1 comes back

        watchDog.watch();

        await().atMost(3, SECONDS).until(() -> doorControlClient.isLocked());
        Thread.sleep(3000); // wait for the time until both person leave the view
        Thread.sleep(DOOR_LOCK_DURATION.plusSeconds(WAIT_TOLERANCE_SEC).toMillis());
        assertThat(doorControlClient.isLocked(), equalTo(true));
    }

    private FaceRecognitionEvent createEvent(final String personId, final long startTime, final long endTime) {
        return new FaceRecognitionEvent("any id", "any camera", personId, startTime, endTime, "any id class");
    }
}
