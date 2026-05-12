package io.github.valossa515.pricetracker.alert.dto;

import java.math.BigDecimal;

public record ProductComparisonResponse(
        String marketplace,
        String name,
        BigDecimal price,
        String url,
        double matchScore,
        BigDecimal priceDiff,
        Double priceDiffPercent
) {
}
