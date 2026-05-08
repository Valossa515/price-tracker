package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.dto.AlertResponse;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateAlertTargetPriceCommand(
        UUID alertId,
        String userId,
        BigDecimal newTargetPrice
) implements ICommand<AlertResponse>, OwnedRequest {
}
