package io.github.valossa515.pricetracker.consent.dto;

import io.github.valossa515.pricetracker.consent.ConsentDocumentType;

public record PendingConsentResponse(
        ConsentDocumentType documentType,
        String version,
        String title,
        String url
) {
}
