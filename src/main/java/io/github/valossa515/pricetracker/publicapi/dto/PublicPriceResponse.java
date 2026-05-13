package io.github.valossa515.pricetracker.publicapi.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PublicPriceResponse(
        String productUrl,
        String productName,
        BigDecimal currentPrice,
        boolean available,
        Instant fetchedAt
) {
}
