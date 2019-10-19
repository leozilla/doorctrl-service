package at.mechatron.doorctrlservice.facerecognition.safrapi.dto;

public class FaceRecognitionEvent {
    private String eventId;
    private String sourceId;
    private String personId;
    private long startTime;
    private long endTime;
    private String idClass;

    public FaceRecognitionEvent() {
    }

    public FaceRecognitionEvent(String eventId, String sourceId, String personId, long startTime, long endTime, String idClass) {
        this.eventId = eventId;
        this.sourceId = sourceId;
        this.personId = personId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.idClass = idClass;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSourceId() {
        return sourceId;
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

    public boolean isInView() {
        return endTime == 0;
    }
}
