package io.github.valossa515.pricetracker.alert.dto;

import io.github.valossa515.pricetracker.alert.AlertType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

public record CreateAlertRequest(

        @NotBlank
        @URL(protocol = "https")
        @Size(max = 2048)
        String productUrl,

        @DecimalMin(value = "0.01", message = "Target price must be greater than zero")
        BigDecimal targetPrice,

        @Size(max = 500)
        String productName,

        AlertType alertType,

        @DecimalMin("1.00") @DecimalMax("99.00")
        BigDecimal discountPercent,

        Integer dropWindowDays,

        @DecimalMin("1.00") @DecimalMax("99.00")
        BigDecimal dropPercent
) {

    public AlertType resolvedType() {
        return alertType == null ? AlertType.PRICE_BELOW_TARGET : alertType;
    }

    @AssertTrue(message = "alert configuration is invalid for the chosen type")
    public boolean isConfigurationValid() {
        return switch (resolvedType()) {
            case PRICE_BELOW_TARGET -> targetPrice != null;
            case PERCENT_DISCOUNT -> discountPercent != null;
            case PRICE_DROP -> dropPercent != null
                    && dropWindowDays != null
                    && dropWindowDays >= 1
                    && dropWindowDays <= 365;
            case BACK_IN_STOCK -> true;
        };
    }
}
