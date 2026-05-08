package io.github.valossa515.pricetracker.pricecheck.handlers;

import io.github.valossa515.pricetracker.pricecheck.PriceTargetReachedEvent;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Placeholder notification handler. Will be joined by SendEmailHandler in leva 5.
 */
@Slf4j
@Service
public class LogPriceTargetReachedHandler implements NotificationHandler<PriceTargetReachedEvent> {

    @Override
    public void handle(PriceTargetReachedEvent event) {
        log.info("PRICE ALERT: alertId={} user={} observed={} target={} url={}",
                event.alertId(),
                event.userId(),
                event.observedPrice(),
                event.targetPrice(),
                event.productUrl());
    }
}
