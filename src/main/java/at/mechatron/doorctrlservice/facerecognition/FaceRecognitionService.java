package at.mechatron.doorctrlservice.facerecognition;

import java.time.Instant;

public interface FaceRecognitionService {

    interface Handler {
        void onFaceRecognized(String personId, Instant startTime);
        void onFaceLost(String personId, Instant endTime);
    }

    void register(final Handler handler);
}
