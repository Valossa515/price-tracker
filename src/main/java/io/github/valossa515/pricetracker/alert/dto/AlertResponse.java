package io.github.valossa515.pricetracker.alert.dto;

import io.github.valossa515.pricetracker.alert.Alert;
import io.github.valossa515.pricetracker.alert.AlertStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        String productUrl,
        String productName,
        BigDecimal targetPrice,
        BigDecimal lastObservedPrice,
        AlertStatus status,
        Instant createdAt,
        Instant lastCheckedAt
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getProductUrl(),
                alert.getProductName(),
                alert.getTargetPrice(),
                alert.getLastObservedPrice(),
                alert.getStatus(),
                alert.getCreatedAt(),
                alert.getLastCheckedAt()
        );
    }
}
