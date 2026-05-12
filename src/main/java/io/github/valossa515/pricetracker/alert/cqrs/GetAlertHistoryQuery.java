package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.dto.PriceHistoryPointResponse;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;

import java.util.List;
import java.util.UUID;

public record GetAlertHistoryQuery(UUID alertId, String userId, int days, int limit)
        implements IQuery<List<PriceHistoryPointResponse>>, OwnedRequest {
}
