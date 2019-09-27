package at.mechatron.doorctrlservice;

import at.mechatron.doorctrlservice.doorctrl.ModbusDoorControlClient;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class ModbusDoorControlTestClient {

    public static void main(final String[] args) throws IOException, InterruptedException {
        final String host = args[0];

        ModbusDoorControlClient client = new ModbusDoorControlClient(host, Executors.newSingleThreadExecutor());
        Scanner scanner = new Scanner(System.in);

        while (true) {
            client.lockDoor();
            Thread.sleep(1000);
            client.unlockDoor();
            // client.getEvents(Instant.now());

            String line = scanner.nextLine();
            if (line.contains("n")) System.exit(0);
        }
    }
}
