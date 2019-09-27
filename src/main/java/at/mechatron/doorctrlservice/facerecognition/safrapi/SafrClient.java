package at.mechatron.doorctrlservice.facerecognition.safrapi;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SafrClient {
    CompletableFuture<List<FaceRecognitionEvent>> getEvents(final Instant startTime);
}
