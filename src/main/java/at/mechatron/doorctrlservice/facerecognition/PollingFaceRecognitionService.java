package at.mechatron.doorctrlservice.facerecognition;

import at.mechatron.doorctrlservice.facerecognition.safrapi.FaceRecognitionEvent;
import at.mechatron.doorctrlservice.facerecognition.safrapi.SAFRClient;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PollingFaceRecognitionService implements FaceRecognitionService {
    private static final Logger LOG = LogManager.getLogger();

    private final Duration pollInterval;
    private final ScheduledExecutorService scheduler;
    private final SAFRClient safrClient;
    private final Executor eventLoop;

    private Set<String> personsInViewPreviously = new HashSet<>();
    private Instant lastPollTime = Instant.MIN;
    private Handler handler;

    public PollingFaceRecognitionService(
            final Duration pollInterval,
            final SAFRClient safrClient,
            final ScheduledExecutorService scheduler,
            final Executor eventLoop) {
        this.pollInterval = pollInterval;
        this.scheduler = scheduler;
        this.safrClient = safrClient;
        this.eventLoop = eventLoop;
    }

    @Override
    public void register(final Handler handler) {
        this.handler = handler;

        this.scheduler.scheduleAtFixedRate(() -> eventLoop.execute(this::pollNow), 0, pollInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void pollNow() {
        Instant sinceTime = lastPollTime == Instant.MIN ? Instant.now().minusSeconds(3) : lastPollTime;
        lastPollTime = Instant.now();

        LOG.debug("Polling for face recognition events. Last poll time/used since time: {}", sinceTime);

        safrClient.getEvents(sinceTime).whenCompleteAsync((faceRecognitionEvents, throwable) -> {
            if (throwable == null) {
                this.handleEvents(faceRecognitionEvents);
            }
        }, eventLoop);
    }

    void handleEvents(final List<FaceRecognitionEvent> faceRecognitionEvents) {
        LOG.debug("Got {} face recognition event(s)", faceRecognitionEvents.size());

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
