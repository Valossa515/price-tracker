package io.github.valossa515.pricetracker.account.dto;

import java.time.Instant;
import java.util.List;

public record AccountExportResponse(
        Instant exportedAt,
        String formatVersion,
        Account account,
        List<Alert> alerts,
        List<Consent> consents
) {
    public record Account(String userId, String email) {}

    public record Alert(
            String id,
            String productUrl,
            String productName,
            String targetPrice,
            String lastObservedPrice,
            String status,
            Instant createdAt,
            Instant lastCheckedAt
    ) {}

    public record Consent(
            String id,
            String documentType,
            String version,
            Instant acceptedAt,
            String ipAddress,
            String userAgent
    ) {}
}
