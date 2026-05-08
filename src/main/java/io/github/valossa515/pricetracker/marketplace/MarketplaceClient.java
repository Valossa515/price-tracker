package io.github.valossa515.pricetracker.marketplace;

import java.util.Optional;

public interface MarketplaceClient {
    String hostKey();
    Optional<ProductInfo> fetchProduct(String url);
}
