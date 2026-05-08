package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteAlertHandler implements CommandHandler<DeleteAlertCommand, Void> {

    private final AlertRepository repository;

    @Override
    @Transactional
    public Void handle(DeleteAlertCommand cmd) {
        repository.deleteById(cmd.alertId());
        return null;
    }
}
