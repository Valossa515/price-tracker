package io.github.valossa515.pricetracker.consent.dto;

import io.github.valossa515.pricetracker.consent.ConsentDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AcceptConsentRequest(
        @NotNull ConsentDocumentType documentType,
        @NotBlank String version
) {
}
