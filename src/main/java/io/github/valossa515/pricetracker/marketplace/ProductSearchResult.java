package io.github.valossa515.pricetracker.marketplace;

import java.math.BigDecimal;

/** Lightweight projection returned by marketplace search APIs. */
public record ProductSearchResult(
        String marketplace,
        String name,
        BigDecimal price,
        String url
) {
}
