package io.github.valossa515.pricetracker.consent.cqrs;

import io.github.valossa515.pricetracker.consent.dto.PendingConsentResponse;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;

import java.util.List;

public record GetRequiredConsentsQuery(String userId) implements IQuery<List<PendingConsentResponse>> {
}
