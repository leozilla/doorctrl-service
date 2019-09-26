package at.mechatron.doorctrlservice;

import at.mechatron.doorctrlservice.doorctrl.DoorControlClient;
import at.mechatron.doorctrlservice.facerecognition.FaceRecognitionService;

import java.time.Instant;

public class WatchDog {
    private final DoorControlClient doorControlClient;
    private final FaceRecognitionService faceRecognitionService;

    public WatchDog(DoorControlClient doorControlClient, FaceRecognitionService faceRecognitionService) {
        this.doorControlClient = doorControlClient;
        this.faceRecognitionService = faceRecognitionService;
    }

    public void watch() {
        this.faceRecognitionService.register(new FaceRecognitionService.Handler() {
            @Override
            public void onFaceRecognized(String personId, Instant startTime) {

            }

            @Override
            public void onFaceLost(String personId, Instant endTime) {

            }
        });
    }
}
