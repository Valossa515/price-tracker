package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.dto.AlertResponse;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;

import java.util.List;

public record ListMyAlertsQuery(String userId) implements IQuery<List<AlertResponse>> {
}
