package io.github.valossa515.pricetracker.alert.dto;

import io.github.valossa515.pricetracker.alert.Alert;
import io.github.valossa515.pricetracker.alert.AlertStatus;
import io.github.valossa515.pricetracker.alert.AlertType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        String productUrl,
        String productName,
        AlertType alertType,
        BigDecimal targetPrice,
        BigDecimal discountPercent,
        Integer dropWindowDays,
        BigDecimal dropPercent,
        BigDecimal lastObservedPrice,
        Boolean lastObservedAvailable,
        Boolean realDiscountFlag,
        AlertStatus status,
        Instant createdAt,
        Instant lastCheckedAt
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getProductUrl(),
                alert.getProductName(),
                alert.getAlertType(),
                alert.getTargetPrice(),
                alert.getDiscountPercent(),
                alert.getDropWindowDays(),
                alert.getDropPercent(),
                alert.getLastObservedPrice(),
                alert.getLastObservedAvailable(),
                alert.getRealDiscountFlag(),
                alert.getStatus(),
                alert.getCreatedAt(),
                alert.getLastCheckedAt()
        );
    }
}
