package at.mechatron.doorctrlservice.facerecognition;

public interface FaceRecognitionService {

    interface Handler {
        void onFaceRecognized(String personId);
        void onFaceLost(String personId);
    }

    void register(final Handler handler);
}
