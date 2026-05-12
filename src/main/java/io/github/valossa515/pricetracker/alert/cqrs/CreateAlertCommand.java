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
        BigDecimal dropPercent
) implements ICommand<AlertResponse> {
}
