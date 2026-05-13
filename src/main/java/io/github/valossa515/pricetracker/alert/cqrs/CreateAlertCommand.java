package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.AlertType;
import io.github.valossa515.pricetracker.alert.dto.AlertResponse;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;

import java.math.BigDecimal;

public record CreateAlertCommand(
        String userId,
        String userEmail,
        String productUrl,
        String productName,
        AlertType alertType,
        BigDecimal targetPrice,
        BigDecimal discountPercent,
        Integer dropWindowDays,
        BigDecimal dropPercent,
        String webhookUrl,
        String webhookSecret
) implements ICommand<AlertResponse> {

    /** Backward-compatible factory for callers that don't use webhooks. */
    public CreateAlertCommand(
            String userId,
            String userEmail,
            String productUrl,
            String productName,
            AlertType alertType,
            BigDecimal targetPrice,
            BigDecimal discountPercent,
            Integer dropWindowDays,
            BigDecimal dropPercent) {
        this(userId, userEmail, productUrl, productName, alertType,
                targetPrice, discountPercent, dropWindowDays, dropPercent,
                null, null);
    }
}
