package io.github.valossa515.pricetracker.pricecheck;

import io.github.valossa515.spring_courier.core.interfaces.INotification;

import java.math.BigDecimal;
import java.util.UUID;

public record PriceTargetReachedEvent(
        UUID alertId,
        String userId,
        String userEmail,
        String productUrl,
        String productName,
        BigDecimal observedPrice,
        BigDecimal targetPrice
) implements INotification {
}
