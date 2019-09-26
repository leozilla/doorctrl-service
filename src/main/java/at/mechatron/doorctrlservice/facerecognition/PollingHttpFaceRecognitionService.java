package at.mechatron.doorctrlservice.facerecognition;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;

import static java.time.temporal.ChronoUnit.SECONDS;

public class PollingHttpFaceRecognitionService implements FaceRecognitionService {
    private static final String RPC_AUTHORIZATION_HEADER = "X-RPC-AUTHORIZATION";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final URI EVENTS_URL;

    private final String authorizationKey;
    private final ScheduledExecutorService scheduler;

    HttpClient client = HttpClient.newHttpClient();
    private Handler handler;

    public PollingHttpFaceRecognitionService(final String baseUrl, final String authorizationKey, final ScheduledExecutorService scheduler) {
        this.EVENTS_URL = baseUrl;
        this.authorizationKey = authorizationKey;
        this.scheduler = scheduler;
    }

    @Override
    public void register(final Handler handler) {
        this.scheduler.scheduleAtFixedRate(this::pollNow, 0, 1, TimeUnit.SECONDS);

        this.handler = handler;
    }

    private void pollNow() {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(EVENTS_URL)
                .timeout(Duration.of(3, SECONDS))
                .header(RPC_AUTHORIZATION_HEADER, this.authorizationKey)
                .header(AUTHORIZATION_HEADER, "main")
                .GET()
                .build();

        CompletableFuture<HttpResponse<String>> response =
                this.client.sendAsync(request, HttpResponse.BodyHandler.asString());
    }
}
