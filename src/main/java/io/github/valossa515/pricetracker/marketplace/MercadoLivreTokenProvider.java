package io.github.valossa515.pricetracker.marketplace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Caches the Mercado Livre app access token (client_credentials flow).
 * Refreshes ~60s before expiry. Thread-safe via double-checked locking.
 */
@Slf4j
@Component
public class MercadoLivreTokenProvider {

    private final RestClient restClient;
    private final String appId;
    private final String clientSecret;

    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private volatile Instant expiresAt = Instant.MIN;
    private final Object lock = new Object();

    public MercadoLivreTokenProvider(
            @Qualifier("mlRestClient") RestClient restClient,
            @Value("${ML_APP_ID}") String appId,
            @Value("${ML_CLIENT_SECRET}") String clientSecret) {
        this.restClient = restClient;
        this.appId = appId;
        this.clientSecret = clientSecret;
    }

    public String getToken() {
        if (Instant.now().isBefore(expiresAt)) {
            return cachedToken.get();
        }
        synchronized (lock) {
            if (Instant.now().isBefore(expiresAt)) {
                return cachedToken.get();
            }
            refresh();
            return cachedToken.get();
        }
    }

    private void refresh() {
        log.info("Refreshing Mercado Livre access token");
        try {
            String body = "grant_type=client_credentials"
                    + "&client_id=" + appId
                    + "&client_secret=" + clientSecret;
            TokenResponse resp = restClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(TokenResponse.class);
            if (resp == null || resp.accessToken == null) {
                throw new IllegalStateException("ML token response missing access_token");
            }
            cachedToken.set(resp.accessToken);
            // 60s safety margin
            expiresAt = Instant.now().plusSeconds(Math.max(60, resp.expiresIn - 60));
        } catch (RestClientException e) {
            log.error("Failed to refresh ML token: {}", e.getMessage());
            throw new IllegalStateException("Could not obtain ML access token", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }
}
