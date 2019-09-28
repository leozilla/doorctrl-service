package at.mechatron.doorctrlservice.facerecognition.safrapi;

import at.mechatron.doorctrlservice.facerecognition.safrapi.dto.FaceRecognitionEvent;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SAFRClient {
    CompletableFuture<List<FaceRecognitionEvent>> getEvents(final Instant sinceTime);
}
