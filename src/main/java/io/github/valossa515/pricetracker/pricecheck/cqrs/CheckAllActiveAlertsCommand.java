package io.github.valossa515.pricetracker.pricecheck.cqrs;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;

public record CheckAllActiveAlertsCommand() implements ICommand<Integer> {
}
