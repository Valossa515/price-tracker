package io.github.valossa515.pricetracker.publicapi.webhook;

import io.github.valossa515.pricetracker.alert.Alert;
import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.pricecheck.PriceTargetReachedEvent;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Delivers a webhook to alerts that have a {@code webhookUrl} set
 * whenever the price target is reached.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookOnPriceTargetReachedHandler
        implements NotificationHandler<PriceTargetReachedEvent> {

    private final AlertRepository alertRepository;
    private final WebhookDeliveryService delivery;

    @Override
    @Transactional(readOnly = true)
    public void handle(PriceTargetReachedEvent event) {
        Alert alert = alertRepository.findById(event.alertId()).orElse(null);
        if (alert == null
                || alert.getWebhookUrl() == null || alert.getWebhookUrl().isBlank()
                || alert.getWebhookSecret() == null || alert.getWebhookSecret().isBlank()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "price.target_reached");
        payload.put("alertId", event.alertId().toString());
        payload.put("productUrl", event.productUrl());
        payload.put("productName", event.productName());
        payload.put("observedPrice", event.observedPrice());
        payload.put("triggerPrice", event.targetPrice());

        delivery.deliver(alert.getWebhookUrl(), alert.getWebhookSecret(),
                "price.target_reached", payload);
    }
}
