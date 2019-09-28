package at.mechatron.doorctrlservice;

import at.mechatron.doorctrlservice.doorctrl.DoorControlClient;
import at.mechatron.doorctrlservice.facerecognition.FaceRecognitionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WatchDog {
    private static final Logger LOG = LogManager.getLogger();

    private final Duration doorLockDuration;
    private final DoorControlClient doorControlClient;
    private final FaceRecognitionService faceRecognitionService;
    private final ScheduledExecutorService scheduler;
    private final Executor eventLoop;

    @Nullable
    private ScheduledFuture<?> scheduleUnlock;

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
            LOG.info("Registered person(s) in view: {}, LOCKING door now", personsNewInView);
            this.doorControlClient.lockDoor();

            if (this.scheduleUnlock != null) {
                LOG.debug("Canceling unlock action");
                this.scheduleUnlock.cancel(true);
                this.scheduleUnlock = null;
            }
        }

        if (!personsNowOutOfView.isEmpty()) {
            this.scheduleUnlock = this.scheduler.schedule(() -> eventLoop.execute(this::unlockDoor), doorLockDuration.getSeconds(), TimeUnit.SECONDS);
        }
    }

    private void unlockDoor() {
        LOG.info("Door lock duration: {} elapsed and no person within field of view, UNLOCKING door now", this.doorLockDuration);
        this.doorControlClient.unlockDoor();
        this.scheduleUnlock = null;
    }
}
