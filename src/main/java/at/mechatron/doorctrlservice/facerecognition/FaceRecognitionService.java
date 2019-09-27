package at.mechatron.doorctrlservice.facerecognition;

import java.util.Set;

public interface FaceRecognitionService {

    interface Handler {
        void onFaceRecognition(Set<String> personsNewInView, Set<String> personsNowOutOfView);
    }

    void register(final Handler handler);
}
