package at.mechatron.doorctrlservice.facerecognition;

import at.mechatron.doorctrlservice.facerecognition.safrapi.FaceRecognitionEvent;
import at.mechatron.doorctrlservice.facerecognition.safrapi.SafrClient;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PollingFaceRecognitionService implements FaceRecognitionService {
    private static final Logger LOG = LogManager.getLogger();

    private final ScheduledExecutorService scheduler;
    private final SafrClient safrClient;
    private final Executor eventLoop;

    private Set<String> personsInViewPreviously = new HashSet<>();
    private Instant lastPollTime = Instant.MIN;
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
        Instant sinceTime = lastPollTime == Instant.MIN ? Instant.now().minusSeconds(3) : lastPollTime;
        lastPollTime = Instant.now();

        LOG.debug("Polling for face recognition events. Last poll time/used since time: {}", sinceTime);

        safrClient.getEvents(sinceTime).thenAcceptAsync(this::handleEvents, eventLoop);
    }

    void handleEvents(final List<FaceRecognitionEvent> faceRecognitionEvents) {
        LOG.debug("Got {} face recognition events", faceRecognitionEvents.size());

        List<FaceRecognitionEvent> sortedEvents = faceRecognitionEvents.stream()
                .sorted(Comparator.comparing(FaceRecognitionEvent::getStartTime))
                .collect(Collectors.toList());

        Set<String> currentPersonsInView = sortedEvents.stream()
                .filter(p -> p.isInView())
                .map(p -> p.getPersonId())
                .collect(Collectors.toSet());
        Set<String> personsNewInView = Sets.difference(currentPersonsInView, personsInViewPreviously);
        Set<String> personsNowOutOfView = Sets.difference(personsInViewPreviously, currentPersonsInView);

        this.handler.onFaceRecognition(personsNewInView, personsNowOutOfView);

        this.personsInViewPreviously = currentPersonsInView;
    }
}
