package io.github.valossa515.pricetracker.alert.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateAlertTargetPriceRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "Target price must be greater than zero")
        BigDecimal targetPrice
) {
}
