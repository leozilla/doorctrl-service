package at.mechatron.doorctrlservice.facerecognition.safrapi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

public class SafrHttpClient implements SafrClient {
    private static final Logger LOG = LogManager.getLogger();

    private static final String RPC_AUTHORIZATION_HEADER = "X-RPC-AUTHORIZATION";
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final URI EVENTS_URL;
    private final HttpRequestFactory httpRequestFactory;
    private final Executor ioExecutor;

    public SafrHttpClient(final String baseUrl, final String authorizationKey, final Duration timeout, final Executor ioExecutor) {
        this.EVENTS_URL = URI.create(baseUrl);
        this.ioExecutor = ioExecutor;

        this.httpRequestFactory =
                HTTP_TRANSPORT.createRequestFactory(
                        request -> {
                            HttpHeaders headers = new HttpHeaders()
                                    .set(RPC_AUTHORIZATION_HEADER, authorizationKey)
                                    .setAuthorization("main");
                            request.setHeaders(headers)
                                    .setConnectTimeout((int) timeout.toMillis())
                                    .setNumberOfRetries(0)
                                    .setReadTimeout((int) timeout.toMillis());
                        });
    }

    public CompletableFuture<List<FaceRecognitionEvent>> getEvents(final Instant startTime) {
        GenericUrl url = new GenericUrl(String.format("%s/events?sinceTime=%d", EVENTS_URL, startTime.toEpochMilli()));

        LOG.debug("HTTP GET {}", url);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = httpRequestFactory
                        .buildGetRequest(url)
                        .execute()
                        .parseAsString();

                LOG.debug("HTTP GET {} returned {}", url, response);

                return mapper.readValue(response, FaceRecognitionEventsResponse.class).getEvents();
            } catch (IOException e) {
                LOG.error("HTTP GET {} failed", url, e);
                throw new RuntimeException(e);
            }
        }, ioExecutor);
    }
}
