package at.mechatron.doorctrlservice.facerecognition.safrapi;

public class FaceRecognitionEvent {
    private String eventId;
    private String personId;
    private long startTime;
    private long endTime;
    private String idClass;

    public String getEventId() {
        return eventId;
    }

    public String getPersonId() {
        return personId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getIdClass() {
        return idClass;
    }
}
