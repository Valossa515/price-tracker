package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.alert.dto.AlertAnalyticsResponse;
import io.github.valossa515.pricetracker.pricecheck.PriceCheckHistory;
import io.github.valossa515.pricetracker.pricecheck.PriceCheckHistoryRepository;
import io.github.valossa515.pricetracker.pricecheck.PriceCheckHistoryRepository.PriceStats;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAlertAnalyticsHandler
        implements QueryHandler<GetAlertAnalyticsQuery, AlertAnalyticsResponse> {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 365;
    /** Discount must beat the recent average by this fraction to count as "real". */
    private static final BigDecimal REAL_DISCOUNT_THRESHOLD = new BigDecimal("0.95");
    /** Trend window: split last N days in half and compare averages. */
    private static final BigDecimal TREND_DEADBAND = new BigDecimal("0.02");

    private final AlertRepository alertRepository;
    private final PriceCheckHistoryRepository historyRepository;

    @Override
    @Transactional(readOnly = true)
    public AlertAnalyticsResponse handle(GetAlertAnalyticsQuery query) {
        int days = clamp(query.days() <= 0 ? DEFAULT_DAYS : query.days(), 1, MAX_DAYS);
        var alert = alertRepository.findById(query.alertId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Instant now = Instant.now();
        Instant since = now.minus(days, ChronoUnit.DAYS);
        PriceStats stats = historyRepository.aggregateSince(query.alertId(), since);
        BigDecimal current = alert.getLastObservedPrice();

        BigDecimal min = stats != null ? stats.getMinPrice() : null;
        BigDecimal max = stats != null ? stats.getMaxPrice() : null;
        BigDecimal avg = stats != null && stats.getAvgPrice() != null
                ? stats.getAvgPrice().setScale(2, RoundingMode.HALF_UP)
                : null;
        long samples = stats != null && stats.getSamples() != null ? stats.getSamples() : 0L;

        boolean isLowest = current != null && min != null && current.compareTo(min) <= 0;
        boolean isReal = current != null && avg != null
                && current.compareTo(avg.multiply(REAL_DISCOUNT_THRESHOLD)) < 0;

        AlertAnalyticsResponse.Trend trend = computeTrend(query.alertId(), now, days);

        return new AlertAnalyticsResponse(
                days,
                samples,
                current,
                min,
                max,
                avg,
                min,
                isLowest,
                isReal,
                trend
        );
    }

    private AlertAnalyticsResponse.Trend computeTrend(java.util.UUID alertId, Instant now, int days) {
        int half = Math.max(1, days / 2);
        Instant midpoint = now.minus(half, ChronoUnit.DAYS);
        Instant start = now.minus((long) half * 2, ChronoUnit.DAYS);

        BigDecimal recent = average(alertId, midpoint, now);
        BigDecimal previous = average(alertId, start, midpoint);
        if (recent == null || previous == null || previous.signum() == 0) {
            return AlertAnalyticsResponse.Trend.UNKNOWN;
        }
        BigDecimal delta = recent.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP);
        if (delta.abs().compareTo(TREND_DEADBAND) <= 0) {
            return AlertAnalyticsResponse.Trend.STABLE;
        }
        return delta.signum() > 0
                ? AlertAnalyticsResponse.Trend.UP
                : AlertAnalyticsResponse.Trend.DOWN;
    }

    private BigDecimal average(java.util.UUID alertId, Instant from, Instant to) {
        List<PriceCheckHistory> rows = historyRepository
                .findByAlertIdAndObservedAtGreaterThanEqualOrderByObservedAtDesc(
                        alertId, from, PageRequest.of(0, 1000));
        BigDecimal sum = BigDecimal.ZERO;
        long count = 0;
        for (PriceCheckHistory h : rows) {
            if (h.getObservedAt().isBefore(to)) {
                sum = sum.add(h.getObservedPrice());
                count++;
            }
        }
        if (count == 0) return null;
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
