package io.github.valossa515.pricetracker.publicapi.dto;

import io.github.valossa515.pricetracker.alert.Alert;
import io.github.valossa515.pricetracker.alert.AlertStatus;
import io.github.valossa515.pricetracker.alert.AlertType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PublicAlertResponse(
        UUID id,
        String productUrl,
        String productName,
        AlertType alertType,
        AlertStatus status,
        BigDecimal targetPrice,
        BigDecimal discountPercent,
        Integer dropWindowDays,
        BigDecimal dropPercent,
        BigDecimal lastObservedPrice,
        Boolean lastObservedAvailable,
        Instant createdAt,
        Instant lastCheckedAt,
        boolean webhookConfigured
) {
    public static PublicAlertResponse from(Alert a) {
        return new PublicAlertResponse(
                a.getId(),
                a.getProductUrl(),
                a.getProductName(),
                a.getAlertType(),
                a.getStatus(),
                a.getTargetPrice(),
                a.getDiscountPercent(),
                a.getDropWindowDays(),
                a.getDropPercent(),
                a.getLastObservedPrice(),
                a.getLastObservedAvailable(),
                a.getCreatedAt(),
                a.getLastCheckedAt(),
                a.getWebhookUrl() != null && !a.getWebhookUrl().isBlank()
        );
    }
}
