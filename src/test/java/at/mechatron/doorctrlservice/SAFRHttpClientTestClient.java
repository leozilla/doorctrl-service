package at.mechatron.doorctrlservice;

import at.mechatron.doorctrlservice.facerecognition.safrapi.SAFRHttpClient;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class SAFRHttpClientTestClient {

    public static void main(final String[] args) throws IOException {
        final String baseUrl = args[0];
        final String authorizationKey = args[1];
        final String eventsSource = args.length >= 3 ? args[2] : "";

        SAFRHttpClient client = new SAFRHttpClient(baseUrl, authorizationKey, Duration.ofSeconds(3), Executors.newSingleThreadExecutor());
        Scanner scanner = new Scanner(System.in);

        while (true) {
            // client.getEvents(eventsSource, Instant.now().minusSeconds(60 * 60 * 120));
            client.getEvents(eventsSource, Instant.now().minusSeconds(5)).whenComplete((events, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                } else {
                    System.out.println(events.size() + " events");
                }
            });

            String line = scanner.nextLine();
            if (line.contains("n")) System.exit(0);
        }
    }
}
