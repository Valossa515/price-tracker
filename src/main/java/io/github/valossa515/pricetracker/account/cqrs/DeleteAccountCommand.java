package io.github.valossa515.pricetracker.account.cqrs;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;

public record DeleteAccountCommand(String userId) implements ICommand<Void> {
}
