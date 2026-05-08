package io.github.valossa515.pricetracker.alert.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

public record CreateAlertRequest(

        @NotBlank
        @URL(protocol = "https")
        @Size(max = 2048)
        String productUrl,

        @NotNull
        @DecimalMin(value = "0.01", message = "Target price must be greater than zero")
        BigDecimal targetPrice,

        @Size(max = 500)
        String productName
) {
}
