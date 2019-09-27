package at.mechatron.doorctrlservice.doorctrl;

import java.util.concurrent.CompletableFuture;

public interface DoorControlClient {

    CompletableFuture<Void> lockDoor();

    CompletableFuture<Void> unlockDoor();
}
