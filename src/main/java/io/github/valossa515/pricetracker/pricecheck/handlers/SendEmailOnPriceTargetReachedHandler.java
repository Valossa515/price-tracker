package io.github.valossa515.pricetracker.pricecheck.handlers;

import io.github.valossa515.pricetracker.notification.EmailService;
import io.github.valossa515.pricetracker.pricecheck.PriceTargetReachedEvent;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendEmailOnPriceTargetReachedHandler implements NotificationHandler<PriceTargetReachedEvent> {

    private final EmailService emailService;

    @Override
    public void handle(PriceTargetReachedEvent event) {
        if (event.userEmail() == null || event.userEmail().isBlank()) {
            return; // webhook-only alert
        }
        emailService.sendPriceAlert(
                event.userEmail(),
                event.productName(),
                event.targetPrice(),
                event.observedPrice(),
                event.productUrl()
        );
    }
}
