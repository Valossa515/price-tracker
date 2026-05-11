package io.github.valossa515.pricetracker.consent.cqrs;

import io.github.valossa515.pricetracker.consent.ConsentDocumentType;
import io.github.valossa515.pricetracker.consent.dto.AcceptConsentResponse;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;

public record AcceptConsentCommand(
        String userId,
        ConsentDocumentType documentType,
        String version,
        String ipAddress,
        String userAgent
) implements ICommand<AcceptConsentResponse> {
}
