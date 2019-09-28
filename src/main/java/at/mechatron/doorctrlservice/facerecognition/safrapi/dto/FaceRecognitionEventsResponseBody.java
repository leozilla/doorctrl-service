package at.mechatron.doorctrlservice.facerecognition.safrapi.dto;

import java.util.List;

public class FaceRecognitionEventsResponseBody {
    private List<FaceRecognitionEvent> events;

    public List<FaceRecognitionEvent> getEvents() {
        return events;
    }
}
