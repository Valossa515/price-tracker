package io.github.valossa515.pricetracker.publicapi.dto;

import io.github.valossa515.pricetracker.alert.AlertType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

public record PublicCreateAlertRequest(

        @NotBlank
        @URL(protocol = "https")
        @Size(max = 2048)
        String productUrl,

        @Size(max = 500)
        String productName,

        AlertType alertType,

        @DecimalMin(value = "0.01", message = "Target price must be greater than zero")
        BigDecimal targetPrice,

        @DecimalMin("1.00") @DecimalMax("99.00")
        BigDecimal discountPercent,

        Integer dropWindowDays,

        @DecimalMin("1.00") @DecimalMax("99.00")
        BigDecimal dropPercent,

        /** Optional contact for legacy email notifications; if absent, only the webhook is fired. */
        @Email @Size(max = 320)
        String ownerEmail,

        /** Optional HTTPS URL the platform will POST to when this alert triggers. */
        @URL(regexp = "^https?://.*")
        @Size(max = 2048)
        String webhookUrl,

        /**
         * Optional shared secret used to compute the HMAC-SHA256 signature
         * sent in the {@code X-PriceTracker-Signature} header. Required if
         * {@code webhookUrl} is provided.
         */
        @Size(min = 16, max = 128)
        String webhookSecret
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

    @AssertTrue(message = "webhookSecret is required when webhookUrl is provided")
    public boolean isWebhookValid() {
        return webhookUrl == null || webhookUrl.isBlank()
                || (webhookSecret != null && !webhookSecret.isBlank());
    }
}
