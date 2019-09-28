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
    private static final Logger LOG = LogManager.getLogger(WatchDog.class);

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

    void onFaceRecognition(final Set<String> personsInView, final Set<String> personsNewInView, final Set<String> personsNowOutOfView) {
        if (!personsInView.isEmpty()) {
            LOG.info("Registered person(s) in view: {}, Person(s) new in view: {}, LOCKING door now", personsInView, personsNewInView);
            this.doorControlClient.lockDoor();

            if (this.scheduleUnlock != null) {
                LOG.debug("Canceling unlock action");
                this.scheduleUnlock.cancel(true);
                this.scheduleUnlock = null;
            }
        } else if (!personsNowOutOfView.isEmpty()) {
            LOG.info("Last person(s) left view: {}. Scheduling unlock in {}", personsNowOutOfView, doorLockDuration);
            this.scheduleUnlock = this.scheduler.schedule(() -> eventLoop.execute(this::unlockDoor), doorLockDuration.getSeconds(), TimeUnit.SECONDS);
        }
    }

    private void unlockDoor() {
        LOG.info("Door lock duration: {} elapsed and no person within field of view, UNLOCKING door now", this.doorLockDuration);
        this.doorControlClient.unlockDoor();
        this.scheduleUnlock = null;
    }
}
