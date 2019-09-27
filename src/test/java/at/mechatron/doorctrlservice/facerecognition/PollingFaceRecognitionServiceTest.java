package at.mechatron.doorctrlservice.facerecognition;

import at.mechatron.doorctrlservice.facerecognition.safrapi.FaceRecognitionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Mockito.*;

class PollingFaceRecognitionServiceTest {

    private PollingFaceRecognitionService sut;
    private FaceRecognitionService.Handler handler;

    @BeforeEach
    public void beforeEach() {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        this.sut = new PollingFaceRecognitionService(null, executor, null);

        this.handler = mock(FaceRecognitionService.Handler.class);
        this.sut.register(handler);
    }

    @Test
    public void newPersonInView() {
        FaceRecognitionEvent ev1 = createEvent("1", 0);
        List<FaceRecognitionEvent> events = Arrays.asList(ev1);
        this.sut.handleEvents(events);

        verify(handler).onFaceRecognized("1");
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void personStillInView() {
        List<FaceRecognitionEvent> events = Arrays.asList(createEvent("1", 0));
        this.sut.handleEvents(events);

        events = Arrays.asList(createEvent("1", 0));
        this.sut.handleEvents(events);

        verify(handler, times(1)).onFaceRecognized("1");
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void personWentOutOfView() {
        List<FaceRecognitionEvent> events = Arrays.asList(createEvent("1", 0));
        this.sut.handleEvents(events);

        events = Arrays.asList(createEvent("1", 1));
        this.sut.handleEvents(events);

        InOrder order = inOrder(handler);
        order.verify(handler).onFaceRecognized("1");
        order.verify(handler).onFaceLost("1");
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
        order.verify(handler).onFaceRecognized("1");
        order.verify(handler, times(1)).onFaceLost("1");
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
        order.verify(handler).onFaceRecognized("1");
        order.verify(handler).onFaceRecognized("2");
        order.verify(handler).onFaceLost("1");
        order.verify(handler).onFaceRecognized("1");
        order.verify(handler).onFaceLost("2");
    }

    private FaceRecognitionEvent createEvent(final String personId, final long endTime) {
        return new FaceRecognitionEvent("any id", personId, 0, endTime, "any id class");
    }
}