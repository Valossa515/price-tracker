package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.dto.AlertAnalyticsResponse;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;

import java.util.UUID;

public record GetAlertAnalyticsQuery(UUID alertId, String userId, int days)
        implements IQuery<AlertAnalyticsResponse>, OwnedRequest {
}
