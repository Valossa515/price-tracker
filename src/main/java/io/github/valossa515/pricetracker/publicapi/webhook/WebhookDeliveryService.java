package io.github.valossa515.pricetracker.publicapi.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.valossa515.pricetracker.publicapi.PublicApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;

/** Delivers HMAC-signed JSON payloads to subscriber webhook URLs. */
@Slf4j
@Service
public class WebhookDeliveryService {

    static final String SIGNATURE_HEADER = "X-PriceTracker-Signature";
    static final String EVENT_HEADER = "X-PriceTracker-Event";
    static final String TIMESTAMP_HEADER = "X-PriceTracker-Timestamp";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int maxAttempts;
    private final long retryBackoffMs;

    public WebhookDeliveryService(PublicApiProperties properties) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(properties.webhook().connectTimeoutMs());
        rf.setReadTimeout(properties.webhook().readTimeoutMs());
        this.restClient = RestClient.builder().requestFactory(rf).build();
        this.maxAttempts = properties.webhook().maxAttempts();
        this.retryBackoffMs = properties.webhook().retryBackoffMs();
    }

    public void deliver(String url, String secret, String eventType, Map<String, Object> payload) {
        if (url == null || url.isBlank() || secret == null || secret.isBlank()) {
            return;
        }
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook payload for url={}: {}", url, e.getMessage());
            return;
        }
        String signature = WebhookSigner.sign(secret, body);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(EVENT_HEADER, eventType)
                        .header(TIMESTAMP_HEADER, Instant.now().toString())
                        .header(SIGNATURE_HEADER, signature)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Webhook delivered: event={} url={} attempt={}", eventType, url, attempt);
                return;
            } catch (RestClientException e) {
                if (attempt == maxAttempts) {
                    log.warn("Webhook delivery FAILED after {} attempts: event={} url={} reason={}",
                            attempt, eventType, url, e.getMessage());
                    return;
                }
                long backoff = retryBackoffMs * (1L << (attempt - 1));
                log.info("Webhook attempt {} failed (event={} url={}); retrying in {}ms",
                        attempt, eventType, url, backoff);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
