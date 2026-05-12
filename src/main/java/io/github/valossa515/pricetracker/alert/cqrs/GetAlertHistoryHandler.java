package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.dto.PriceHistoryPointResponse;
import io.github.valossa515.pricetracker.pricecheck.PriceCheckHistoryRepository;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAlertHistoryHandler
        implements QueryHandler<GetAlertHistoryQuery, List<PriceHistoryPointResponse>> {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 365;
    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 5000;

    private final PriceCheckHistoryRepository historyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PriceHistoryPointResponse> handle(GetAlertHistoryQuery query) {
        int days = clamp(query.days() <= 0 ? DEFAULT_DAYS : query.days(), 1, MAX_DAYS);
        int limit = clamp(query.limit() <= 0 ? DEFAULT_LIMIT : query.limit(), 1, MAX_LIMIT);
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return historyRepository
                .findByAlertIdAndObservedAtGreaterThanEqualOrderByObservedAtDesc(
                        query.alertId(), since, PageRequest.of(0, limit))
                .stream()
                .map(PriceHistoryPointResponse::from)
                .toList();
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
