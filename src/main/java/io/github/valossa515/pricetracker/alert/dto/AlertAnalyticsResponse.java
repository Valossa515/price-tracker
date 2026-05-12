package io.github.valossa515.pricetracker.alert.dto;

import java.math.BigDecimal;

public record AlertAnalyticsResponse(
        int periodDays,
        long samples,
        BigDecimal currentPrice,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal avgPrice,
        BigDecimal lowestEverPrice,
        boolean isLowestInPeriod,
        boolean isRealDiscount,
        Trend trend
) {
    public enum Trend { UP, DOWN, STABLE, UNKNOWN }
}
