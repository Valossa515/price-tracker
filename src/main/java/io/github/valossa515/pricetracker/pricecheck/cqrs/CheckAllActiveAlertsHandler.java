package io.github.valossa515.pricetracker.pricecheck.cqrs;

import io.github.valossa515.pricetracker.alert.Alert;
import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.alert.AlertStatus;
import io.github.valossa515.pricetracker.marketplace.ProductInfo;
import io.github.valossa515.pricetracker.marketplace.cqrs.FetchProductPriceQuery;
import io.github.valossa515.pricetracker.pricecheck.PriceTargetReachedEvent;
import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckAllActiveAlertsHandler implements CommandHandler<CheckAllActiveAlertsCommand, Integer> {

    private final AlertRepository repository;
    private final Courier courier;

    @Override
    @Transactional
    public Integer handle(CheckAllActiveAlertsCommand cmd) {
        List<Alert> active = repository.findByStatus(AlertStatus.ACTIVE);
        int processed = 0;
        for (Alert alert : active) {
            try {
                ProductInfo info = courier.send(
                        new FetchProductPriceQuery(alert.getProductUrl())).getData();
                if (info == null) {
                    log.debug("No price returned for alert {}", alert.getId());
                    continue;
                }
                alert.setLastObservedPrice(info.currentPrice());
                alert.setLastCheckedAt(Instant.now());
                if (info.currentPrice().compareTo(alert.getTargetPrice()) <= 0) {
                    alert.setStatus(AlertStatus.TRIGGERED);
                    courier.publish(new PriceTargetReachedEvent(
                            alert.getId(),
                            alert.getUserId(),
                            alert.getUserEmail(),
                            alert.getProductUrl(),
                            alert.getProductName(),
                            info.currentPrice(),
                            alert.getTargetPrice()
                    ));
                }
                processed++;
            } catch (Exception e) {
                log.warn("Failed to check alert {}: {}", alert.getId(), e.getMessage());
            }
        }
        log.info("Price check finished: {} of {} alerts processed", processed, active.size());
        return processed;
    }
}
