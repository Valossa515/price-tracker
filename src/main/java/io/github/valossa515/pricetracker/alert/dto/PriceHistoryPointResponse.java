package io.github.valossa515.pricetracker.alert.dto;

import io.github.valossa515.pricetracker.pricecheck.PriceCheckHistory;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceHistoryPointResponse(BigDecimal price, Instant observedAt) {
    public static PriceHistoryPointResponse from(PriceCheckHistory h) {
        return new PriceHistoryPointResponse(h.getObservedPrice(), h.getObservedAt());
    }
}
