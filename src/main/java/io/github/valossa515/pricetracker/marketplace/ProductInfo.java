package io.github.valossa515.pricetracker.marketplace;

import java.math.BigDecimal;

public record ProductInfo(String name, BigDecimal currentPrice, boolean available) {
    public ProductInfo(String name, BigDecimal currentPrice) {
        this(name, currentPrice, true);
    }
}
