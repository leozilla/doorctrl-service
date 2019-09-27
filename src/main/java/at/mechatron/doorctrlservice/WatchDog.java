package at.mechatron.doorctrlservice;

import at.mechatron.doorctrlservice.doorctrl.DoorControlClient;
import at.mechatron.doorctrlservice.facerecognition.FaceRecognitionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WatchDog {
    private static final Logger LOG = LogManager.getLogger();

    private final Duration doorLockDuration;
    private final DoorControlClient doorControlClient;
    private final FaceRecognitionService faceRecognitionService;
    private final ScheduledExecutorService scheduler;
    private final Executor eventLoop;

    public WatchDog(final Duration doorLockDuration,
                    final DoorControlClient doorControlClient,
                    final FaceRecognitionService faceRecognitionService,
                    final ScheduledExecutorService scheduler,
                    final Executor eventLoop) {
        this.doorLockDuration = doorLockDuration;
        this.doorControlClient = doorControlClient;
        this.faceRecognitionService = faceRecognitionService;
        this.scheduler = scheduler;
        this.eventLoop = eventLoop;
    }

    public void watch() {
        LOG.info("Started watch dog");

        this.faceRecognitionService.register(this::onFaceRecognition);
    }

    void onFaceRecognition(final Set<String> personsNewInView, final Set<String> personsNowOutOfView) {
        if (!personsNewInView.isEmpty()) {
            this.doorControlClient.lockDoor();
        }

        if (!personsNowOutOfView.isEmpty()) {
            this.scheduler.schedule(() -> eventLoop.execute(this::unlockDoor), doorLockDuration.getSeconds(), TimeUnit.SECONDS);
        }
    }

    private void unlockDoor() {
        this.doorControlClient.unlockDoor();
    }
}
