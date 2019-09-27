package at.mechatron.doorctrlservice;

import at.mechatron.doorctrlservice.doorctrl.DoorControlClient;
import at.mechatron.doorctrlservice.facerecognition.FaceRecognitionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

public class WatchDog {
    private static final Logger LOG = LogManager.getLogger();

    private final DoorControlClient doorControlClient;
    private final FaceRecognitionService faceRecognitionService;
    private final ScheduledExecutorService scheduler;
    private final Executor eventLoop;

    public WatchDog(final DoorControlClient doorControlClient,
                    final FaceRecognitionService faceRecognitionService,
                    final ScheduledExecutorService scheduler,
                    final Executor eventLoop) {
        this.doorControlClient = doorControlClient;
        this.faceRecognitionService = faceRecognitionService;
        this.scheduler = scheduler;
        this.eventLoop = eventLoop;
    }

    public void watch() {
        LOG.info("Started watch dog");

        this.faceRecognitionService.register((personsNewInView, personsNowOutOfView) -> {

        });
    }
}
