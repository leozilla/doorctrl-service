package at.mechatron.doorctrlservice.facerecognition;

import java.time.Instant;
import java.util.Optional;

public class FaceRecognition {
    private final String eventId;
    private final String personId;
    private final Instant startTime;
    private final Optional<Instant> endTime;
}
