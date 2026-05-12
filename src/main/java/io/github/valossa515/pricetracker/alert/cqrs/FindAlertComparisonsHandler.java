package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.alert.dto.ProductComparisonResponse;
import io.github.valossa515.pricetracker.marketplace.MarketplaceClient;
import io.github.valossa515.pricetracker.marketplace.MarketplaceResolver;
import io.github.valossa515.pricetracker.marketplace.ProductSearchResult;
import io.github.valossa515.pricetracker.marketplace.ProductTitleMatcher;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FindAlertComparisonsHandler
        implements QueryHandler<FindAlertComparisonsQuery, List<ProductComparisonResponse>> {

    private static final int SEARCH_FETCH_PER_CLIENT = 10;
    private static final double DEFAULT_MIN_SCORE = 0.55;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final AlertRepository alertRepository;
    private final List<MarketplaceClient> clients;
    private final MarketplaceResolver resolver;
    private final ProductTitleMatcher matcher;

    @Override
    @Transactional(readOnly = true)
    public List<ProductComparisonResponse> handle(FindAlertComparisonsQuery query) {
        var alert = alertRepository.findById(query.alertId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String title = alert.getProductName();
        if (title == null || title.isBlank()) {
            return List.of();
        }

        double minScore = query.minScore() <= 0 ? DEFAULT_MIN_SCORE : query.minScore();
        int limit = clamp(query.limit() <= 0 ? DEFAULT_LIMIT : query.limit(), 1, MAX_LIMIT);

        String sourceHost = resolver.resolve(alert.getProductUrl())
                .map(MarketplaceClient::hostKey)
                .orElse(null);
        BigDecimal currentPrice = alert.getLastObservedPrice();

        List<ProductComparisonResponse> matches = new ArrayList<>();
        for (MarketplaceClient client : clients) {
            if (sourceHost != null && sourceHost.equals(client.hostKey())) continue;
            for (ProductSearchResult hit : client.search(title, SEARCH_FETCH_PER_CLIENT)) {
                double score = matcher.similarity(title, hit.name());
                if (score < minScore) continue;
                matches.add(toResponse(hit, score, currentPrice));
            }
        }

        return matches.stream()
                .sorted(Comparator
                        .comparing(ProductComparisonResponse::price, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }

    private ProductComparisonResponse toResponse(ProductSearchResult hit, double score, BigDecimal currentPrice) {
        BigDecimal diff = null;
        Double diffPct = null;
        if (currentPrice != null && hit.price() != null) {
            diff = hit.price().subtract(currentPrice);
            if (currentPrice.signum() != 0) {
                diffPct = diff.divide(currentPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();
            }
        }
        return new ProductComparisonResponse(
                hit.marketplace(),
                hit.name(),
                hit.price(),
                hit.url(),
                Math.round(score * 1000.0) / 1000.0,
                diff,
                diffPct
        );
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
