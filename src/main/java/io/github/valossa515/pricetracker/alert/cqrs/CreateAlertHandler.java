package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.Alert;
import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.alert.AlertType;
import io.github.valossa515.pricetracker.alert.dto.AlertResponse;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAlertHandler implements CommandHandler<CreateAlertCommand, AlertResponse> {

    private final AlertRepository repository;

    @Override
    @Transactional
    public AlertResponse handle(CreateAlertCommand cmd) {
        AlertType type = cmd.alertType() == null ? AlertType.PRICE_BELOW_TARGET : cmd.alertType();
        Alert alert = Alert.builder()
                .userId(cmd.userId())
                .userEmail(cmd.userEmail())
                .productUrl(cmd.productUrl())
                .productName(cmd.productName())
                .alertType(type)
                .targetPrice(cmd.targetPrice())
                .discountPercent(cmd.discountPercent())
                .dropWindowDays(cmd.dropWindowDays())
                .dropPercent(cmd.dropPercent())
                .build();
        return AlertResponse.from(repository.save(alert));
    }
}
