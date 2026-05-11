package io.github.valossa515.pricetracker.consent.dto;

import io.github.valossa515.pricetracker.consent.ConsentDocumentType;

import java.time.Instant;
import java.util.UUID;

public record AcceptConsentResponse(
        UUID id,
        ConsentDocumentType documentType,
        String version,
        Instant acceptedAt
) {
}
