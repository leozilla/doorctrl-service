package at.mechatron.doorctrlservice;

import at.mechatron.doorctrlservice.doorctrl.DoorControlClient;
import at.mechatron.doorctrlservice.facerecognition.FaceRecognitionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.concurrent.Executor;

public class WatchDog {
    private static final Logger LOG = LogManager.getLogger();

    private final DoorControlClient doorControlClient;
    private final FaceRecognitionService faceRecognitionService;
    private final Executor eventLoop;

    public WatchDog(final DoorControlClient doorControlClient, final FaceRecognitionService faceRecognitionService, final Executor eventLoop) {
        this.doorControlClient = doorControlClient;
        this.faceRecognitionService = faceRecognitionService;
        this.eventLoop = eventLoop;
    }

    public void watch() {
        LOG.info("Started watch dog");

        this.faceRecognitionService.register(new FaceRecognitionService.Handler() {
            @Override
            public void onFaceRecognized(String personId, Instant startTime) {

            }

            @Override
            public void onFaceLost(String personId, Instant endTime) {

            }
        });
    }
}
