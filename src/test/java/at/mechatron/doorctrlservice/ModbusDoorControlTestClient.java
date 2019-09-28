package at.mechatron.doorctrlservice;

import at.mechatron.doorctrlservice.doorctrl.ModbusDoorControlClient;

import java.util.Scanner;
import java.util.concurrent.Executors;

public class ModbusDoorControlTestClient {

    public static void main(final String[] args) {
        final String host = args[0];

        ModbusDoorControlClient client = new ModbusDoorControlClient(host, Executors.newSingleThreadExecutor());
        Scanner scanner = new Scanner(System.in);

        while (true) {
            String line = scanner.nextLine();
            if (line.contains("e")) System.exit(0);
            else if (line.contains("l")) client.lockDoor().exceptionally(t -> doorError("LOCK", t));
            else if (line.contains("u")) client.unlockDoor().exceptionally(t -> doorError("UNLOCK", t));
        }
    }

    private static Void doorError(final String status, final Throwable throwable) {
        throwable.printStackTrace();
        return null;
    }
}
