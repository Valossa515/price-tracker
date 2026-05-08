package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.Alert;
import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.alert.AlertStatus;
import io.github.valossa515.pricetracker.alert.dto.AlertResponse;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UpdateAlertTargetPriceHandler implements CommandHandler<UpdateAlertTargetPriceCommand, AlertResponse> {

    private final AlertRepository repository;

    @Override
    @Transactional
    public AlertResponse handle(UpdateAlertTargetPriceCommand cmd) {
        Alert alert = repository.findById(cmd.alertId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        alert.setTargetPrice(cmd.newTargetPrice());
        // Reactivate when raising the target — previously triggered alerts may resume
        if (alert.getStatus() == AlertStatus.TRIGGERED) {
            alert.setStatus(AlertStatus.ACTIVE);
        }
        return AlertResponse.from(alert); // managed entity, JPA dirty-checking flushes on commit
    }
}
