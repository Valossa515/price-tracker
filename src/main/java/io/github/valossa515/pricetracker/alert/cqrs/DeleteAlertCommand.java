package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;

import java.util.UUID;

public record DeleteAlertCommand(UUID alertId, String userId)
        implements ICommand<Void>, OwnedRequest {
}
