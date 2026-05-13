package io.github.valossa515.pricetracker.publicapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.publicapi")
public record PublicApiProperties(
        String requiredScope,
        RateLimit rateLimit,
        Webhook webhook
) {
    public PublicApiProperties {
        if (requiredScope == null || requiredScope.isBlank()) {
            requiredScope = "external-api";
        }
        if (rateLimit == null) rateLimit = new RateLimit(60, 10);
        if (webhook == null) webhook = new Webhook(3000, 5000, 3, 500);
    }

    public record RateLimit(int requestsPerMinute, int burst) {
        public RateLimit {
            if (requestsPerMinute <= 0) requestsPerMinute = 60;
            if (burst <= 0) burst = Math.max(10, requestsPerMinute / 6);
        }
    }

    public record Webhook(int connectTimeoutMs, int readTimeoutMs, int maxAttempts, long retryBackoffMs) {
        public Webhook {
            if (connectTimeoutMs <= 0) connectTimeoutMs = 3000;
            if (readTimeoutMs <= 0) readTimeoutMs = 5000;
            if (maxAttempts <= 0) maxAttempts = 3;
            if (retryBackoffMs <= 0) retryBackoffMs = 500;
        }
    }
}
