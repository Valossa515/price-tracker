package io.github.valossa515.pricetracker.marketplace;

import java.util.List;
import java.util.Optional;

public interface MarketplaceClient {
    String hostKey();
    Optional<ProductInfo> fetchProduct(String url);

    /** Search for similar products by free-text query. Default: not supported. */
    default List<ProductSearchResult> search(String query, int limit) {
        return List.of();
    }
}
