package at.mechatron.doorctrlservice.facerecognition;

import at.mechatron.doorctrlservice.facerecognition.safrapi.dto.FaceRecognitionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.collect.Sets.*;
import static org.mockito.Mockito.*;

class PollingFaceRecognitionServiceTest {

    private PollingFaceRecognitionService sut;
    private FaceRecognitionService.Handler handler;

    @BeforeEach
    public void beforeEach() {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        this.sut = new PollingFaceRecognitionService(Duration.ofSeconds(3), null, executor, null);

        this.handler = mock(FaceRecognitionService.Handler.class);
        this.sut.register(handler);
    }

    @Test
    public void newPersonInView() {
        FaceRecognitionEvent ev1 = createEvent("1", 0);
        List<FaceRecognitionEvent> events = Arrays.asList(ev1);
        this.sut.handleEvents(events);

        verify(handler).onFaceRecognition(newHashSet("1"), newHashSet("1"), newHashSet());
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void personStillInView() {
        List<FaceRecognitionEvent> events = Arrays.asList(createEvent("1", 0));
        this.sut.handleEvents(events);

        events = Arrays.asList(createEvent("1", 0));
        this.sut.handleEvents(events);

        InOrder order = inOrder(handler);
        order.verify(handler).onFaceRecognition(newHashSet("1"), newHashSet("1"), newHashSet());
        order.verify(handler).onFaceRecognition(newHashSet("1"), newHashSet(), newHashSet());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void personWentOutOfView() {
        List<FaceRecognitionEvent> events = Arrays.asList(createEvent("1", 0));
        this.sut.handleEvents(events);

        events = Arrays.asList(createEvent("1", 1));
        this.sut.handleEvents(events);

        InOrder order = inOrder(handler);
        order.verify(handler).onFaceRecognition(newHashSet("1"), newHashSet("1"), newHashSet());
        order.verify(handler).onFaceRecognition(newHashSet(), newHashSet(), newHashSet("1"));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void personStillOutOfView() {
        List<FaceRecognitionEvent> events = Arrays.asList(createEvent("1", 0));
        this.sut.handleEvents(events);

        events = Arrays.asList(createEvent("1", 1));
        this.sut.handleEvents(events);

        events = Arrays.asList(createEvent("1", 1));
        this.sut.handleEvents(events);

        InOrder order = inOrder(handler);
        order.verify(handler).onFaceRecognition(newHashSet("1"), newHashSet("1"), newHashSet());
        order.verify(handler).onFaceRecognition(newHashSet(), newHashSet(), newHashSet("1"));
        order.verify(handler).onFaceRecognition(newHashSet(), newHashSet(), newHashSet());
    }

    @Test
    public void multiplePersons() {
        List<FaceRecognitionEvent> events = Arrays.asList(createEvent("1", 0));
        this.sut.handleEvents(events);

        events = Arrays.asList(createEvent("1", 0), createEvent("2", 0));
        this.sut.handleEvents(events);

        events = Arrays.asList(createEvent("1", 1), createEvent("2", 0));
        this.sut.handleEvents(events);

        events = Arrays.asList(createEvent("1", 0), createEvent("2", 1));
        this.sut.handleEvents(events);

        InOrder order = inOrder(handler);
        order.verify(handler).onFaceRecognition(newHashSet("1"), newHashSet("1"), newHashSet());
        order.verify(handler).onFaceRecognition(newHashSet("1", "2"), newHashSet("2"), newHashSet());
        order.verify(handler).onFaceRecognition(newHashSet("2"), newHashSet(), newHashSet("1"));
        order.verify(handler).onFaceRecognition(newHashSet("1"), newHashSet("1"), newHashSet("2"));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void sortEventsAndTakeOnlyLatest() {
        List<FaceRecognitionEvent> events = Arrays.asList(
                createEvent("1", 3, 4),
                createEvent("1", 5, 0 ),
                createEvent("1", 1, 2));
        this.sut.handleEvents(events);

        verify(handler).onFaceRecognition(newHashSet("1"), newHashSet("1"), newHashSet());
        verifyNoMoreInteractions(handler);
    }

    private FaceRecognitionEvent createEvent(final String personId, final long endTime) {
        return new FaceRecognitionEvent("any id", personId, 0, endTime, "any id class");
    }

    private FaceRecognitionEvent createEvent(final String personId, final long startTime, final long endTime) {
        return new FaceRecognitionEvent("any id", personId, startTime, endTime, "any id class");
    }
}