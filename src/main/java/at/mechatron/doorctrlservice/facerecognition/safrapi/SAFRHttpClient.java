package at.mechatron.doorctrlservice.facerecognition.safrapi;

import at.mechatron.doorctrlservice.facerecognition.safrapi.dto.FaceRecognitionEvent;
import at.mechatron.doorctrlservice.facerecognition.safrapi.dto.FaceRecognitionEventsResponseBody;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SAFRHttpClient implements SAFRClient {
    private static final Logger LOG = LogManager.getLogger(SAFRHttpClient.class);

    private static final String RPC_AUTHORIZATION_HEADER = "X-RPC-AUTHORIZATION";
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final URI eventsUrl;
    private final HttpRequestFactory httpRequestFactory;
    private final Executor ioExecutor;

    public SAFRHttpClient(final String baseUrl, final String authorizationKey, final Duration timeout, final Executor ioExecutor) {
        this.eventsUrl = URI.create(String.format("%s/events", baseUrl));
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
                                    .setReadTimeout((int) timeout.toMillis())
                                    .setWriteTimeout((int) timeout.toMillis());
                        });
    }

    public CompletableFuture<List<FaceRecognitionEvent>> getEvents(final String sourceId, final Instant startTime) {
        String rawUrl = String.format("%s?sinceTime=%d", eventsUrl, startTime.toEpochMilli());

        if (!Strings.isNullOrEmpty(sourceId)) {
            rawUrl = String.format("%s&source=%s", rawUrl, sourceId);
        }

        GenericUrl genericUrl = new GenericUrl(rawUrl);
        String url = genericUrl.build();

        LOG.debug("HTTP GET {}", url);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = httpRequestFactory
                        .buildGetRequest(genericUrl)
                        .execute()
                        .parseAsString();

                LOG.debug("HTTP GET {} returned {}", url, response);

                return mapper.readValue(response, FaceRecognitionEventsResponseBody.class).getEvents();
            } catch (IOException e) {
                LOG.error("HTTP GET {} failed", url, e);
                throw new RuntimeException(e);
            }
        }, ioExecutor);
    }
}
