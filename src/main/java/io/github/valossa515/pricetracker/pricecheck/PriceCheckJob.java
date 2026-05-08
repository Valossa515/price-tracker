package io.github.valossa515.pricetracker.pricecheck;

import io.github.valossa515.pricetracker.pricecheck.cqrs.CheckAllActiveAlertsCommand;
import io.github.valossa515.spring_courier.core.Courier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceCheckJob {

    private final Courier courier;

    @Scheduled(cron = "${app.pricecheck.cron}")
    public void check() {
        log.info("Triggering scheduled price check");
        courier.send(new CheckAllActiveAlertsCommand());
    }
}
