package io.github.valossa515.pricetracker.pricecheck.cqrs;

import io.github.valossa515.pricetracker.alert.Alert;
import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.alert.AlertStatus;
import io.github.valossa515.pricetracker.alert.AlertType;
import io.github.valossa515.pricetracker.marketplace.ProductInfo;
import io.github.valossa515.pricetracker.marketplace.cqrs.FetchProductPriceQuery;
import io.github.valossa515.pricetracker.pricecheck.PriceCheckHistory;
import io.github.valossa515.pricetracker.pricecheck.PriceCheckHistoryRepository;
import io.github.valossa515.pricetracker.pricecheck.PriceCheckHistoryRepository.PriceStats;
import io.github.valossa515.pricetracker.pricecheck.PriceTargetReachedEvent;
import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckAllActiveAlertsHandler implements CommandHandler<CheckAllActiveAlertsCommand, Integer> {

    private static final int FAKE_DISCOUNT_WINDOW_DAYS = 30;
    private static final BigDecimal REAL_DISCOUNT_THRESHOLD = new BigDecimal("0.95");
    /** Tolerance window around the requested cutoff to find a comparable historical price. */
    private static final long DROP_LOOKBACK_TOLERANCE_DAYS = 2;

    private final AlertRepository repository;
    private final PriceCheckHistoryRepository historyRepository;
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

                Instant now = Instant.now();
                Boolean previousAvailable = alert.getLastObservedAvailable();

                alert.setLastObservedPrice(info.currentPrice());
                alert.setLastObservedAvailable(info.available());
                alert.setLastCheckedAt(now);
                alert.setRealDiscountFlag(computeRealDiscountFlag(alert.getId(), info.currentPrice(), now));

                historyRepository.save(PriceCheckHistory.builder()
                        .alertId(alert.getId())
                        .observedPrice(info.currentPrice())
                        .observedAt(now)
                        .available(info.available())
                        .build());

                BigDecimal threshold = evaluateTrigger(alert, info, previousAvailable, now);
                if (threshold != null) {
                    alert.setStatus(AlertStatus.TRIGGERED);
                    courier.publish(new PriceTargetReachedEvent(
                            alert.getId(),
                            alert.getUserId(),
                            alert.getUserEmail(),
                            alert.getProductUrl(),
                            alert.getProductName(),
                            info.currentPrice(),
                            threshold
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

    /**
     * Returns the threshold price that was crossed (for inclusion in the event), or null
     * when the alert should not fire.
     */
    private BigDecimal evaluateTrigger(Alert alert, ProductInfo info, Boolean previousAvailable, Instant now) {
        AlertType type = alert.getAlertType() == null ? AlertType.PRICE_BELOW_TARGET : alert.getAlertType();
        BigDecimal current = info.currentPrice();
        return switch (type) {
            case PRICE_BELOW_TARGET -> {
                BigDecimal target = alert.getTargetPrice();
                yield (target != null && current.compareTo(target) <= 0) ? target : null;
            }
            case PERCENT_DISCOUNT -> {
                if (alert.getDiscountPercent() == null) yield null;
                BigDecimal avg = averagePrice(alert.getId(), now, FAKE_DISCOUNT_WINDOW_DAYS);
                if (avg == null || avg.signum() == 0) yield null;
                BigDecimal factor = BigDecimal.ONE.subtract(
                        alert.getDiscountPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                BigDecimal threshold = avg.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                yield current.compareTo(threshold) <= 0 ? threshold : null;
            }
            case PRICE_DROP -> {
                if (alert.getDropPercent() == null || alert.getDropWindowDays() == null) yield null;
                Optional<PriceCheckHistory> baseline = baselinePrice(alert.getId(), now, alert.getDropWindowDays());
                if (baseline.isEmpty()) yield null;
                BigDecimal factor = BigDecimal.ONE.subtract(
                        alert.getDropPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                BigDecimal threshold = baseline.get().getObservedPrice().multiply(factor)
                        .setScale(2, RoundingMode.HALF_UP);
                yield current.compareTo(threshold) <= 0 ? threshold : null;
            }
            case BACK_IN_STOCK -> (Boolean.FALSE.equals(previousAvailable) && info.available()) ? current : null;
        };
    }

    private Boolean computeRealDiscountFlag(java.util.UUID alertId, BigDecimal currentPrice, Instant now) {
        BigDecimal avg = averagePrice(alertId, now, FAKE_DISCOUNT_WINDOW_DAYS);
        if (avg == null || avg.signum() == 0) return null;
        return currentPrice.compareTo(avg.multiply(REAL_DISCOUNT_THRESHOLD)) < 0;
    }

    private BigDecimal averagePrice(java.util.UUID alertId, Instant now, int days) {
        Instant since = now.minus(days, ChronoUnit.DAYS);
        PriceStats stats = historyRepository.aggregateSince(alertId, since);
        if (stats == null || stats.getAvgPrice() == null) return null;
        return stats.getAvgPrice().setScale(2, RoundingMode.HALF_UP);
    }

    private Optional<PriceCheckHistory> baselinePrice(java.util.UUID alertId, Instant now, int days) {
        Instant cutoff = now.minus(days, ChronoUnit.DAYS);
        Optional<PriceCheckHistory> exact = historyRepository
                .findFirstByAlertIdAndObservedAtLessThanEqualOrderByObservedAtDesc(alertId, cutoff);
        if (exact.isPresent()
                && exact.get().getObservedAt().isAfter(cutoff.minus(DROP_LOOKBACK_TOLERANCE_DAYS, ChronoUnit.DAYS))) {
            return exact;
        }
        return exact;
    }
}
