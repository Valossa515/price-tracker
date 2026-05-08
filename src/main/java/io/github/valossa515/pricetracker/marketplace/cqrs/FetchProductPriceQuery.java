package io.github.valossa515.pricetracker.marketplace.cqrs;

import io.github.valossa515.pricetracker.marketplace.ProductInfo;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;

public record FetchProductPriceQuery(String productUrl) implements IQuery<ProductInfo> {
}
