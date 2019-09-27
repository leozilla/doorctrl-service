package at.mechatron.doorctrlservice.facerecognition;

import at.mechatron.doorctrlservice.facerecognition.safrapi.FaceRecognitionEvent;
import at.mechatron.doorctrlservice.facerecognition.safrapi.SafrClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PollingFaceRecognitionService implements FaceRecognitionService {
    private static final Logger LOG = LogManager.getLogger();

    private final ScheduledExecutorService scheduler;
    private final SafrClient safrClient;
    private final Executor eventLoop;

    private final Map<String, PersonInView> personsInFieldOfView = new HashMap<>();
    private final Map<String, PersonOutOfView> personsOutOfFieldOfView = new HashMap<>();

    private static final class PersonInView {

    }

    private static final class PersonOutOfView {

    }

    private Handler handler;

    public PollingFaceRecognitionService(final SafrClient safrClient, final ScheduledExecutorService scheduler, final Executor eventLoop) {
        this.scheduler = scheduler;
        this.safrClient = safrClient;
        this.eventLoop = eventLoop;
    }

    @Override
    public void register(final Handler handler) {
        this.handler = handler;

        this.scheduler.scheduleAtFixedRate(this::pollNow, 0, 3, TimeUnit.SECONDS);
    }

    private void pollNow() {
        LOG.debug("Polling for face recognition events");

        safrClient.getEvents(Instant.now()).thenAcceptAsync(this::handleEvents, eventLoop);
    }

    private void handleEvents(final List<FaceRecognitionEvent> faceRecognitionEvents) {
        LOG.debug("Got {} face recognition events", faceRecognitionEvents.size());
    }
}
