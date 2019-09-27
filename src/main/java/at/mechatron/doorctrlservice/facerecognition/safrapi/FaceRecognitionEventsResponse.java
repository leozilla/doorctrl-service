package at.mechatron.doorctrlservice.facerecognition.safrapi;

import java.util.List;

public class FaceRecognitionEventsResponse {
    private List<FaceRecognitionEvent> events;

    public List<FaceRecognitionEvent> getEvents() {
        return events;
    }

    public void setEvents(List<FaceRecognitionEvent> events) {
        this.events = events;
    }
}
