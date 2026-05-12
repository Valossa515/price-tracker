package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.dto.ProductComparisonResponse;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;

import java.util.List;
import java.util.UUID;

public record FindAlertComparisonsQuery(UUID alertId, String userId, double minScore, int limit)
        implements IQuery<List<ProductComparisonResponse>>, OwnedRequest {
}
