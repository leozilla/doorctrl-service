package at.mechatron.doorctrlservice.facerecognition.safrapi.dto;

import java.util.ArrayList;
import java.util.List;

public class FaceRecognitionEventsResponseBody {
    private List<FaceRecognitionEvent> events = new ArrayList<>();

    public List<FaceRecognitionEvent> getEvents() {
        return events;
    }
}
