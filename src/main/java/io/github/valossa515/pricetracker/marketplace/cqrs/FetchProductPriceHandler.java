package io.github.valossa515.pricetracker.marketplace.cqrs;

import io.github.valossa515.pricetracker.marketplace.MarketplaceResolver;
import io.github.valossa515.pricetracker.marketplace.ProductInfo;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FetchProductPriceHandler implements QueryHandler<FetchProductPriceQuery, ProductInfo> {

    private final MarketplaceResolver resolver;

    @Override
    public ProductInfo handle(FetchProductPriceQuery query) {
        return resolver.resolve(query.productUrl())
                .flatMap(client -> client.fetchProduct(query.productUrl()))
                .orElse(null);
    }
}
