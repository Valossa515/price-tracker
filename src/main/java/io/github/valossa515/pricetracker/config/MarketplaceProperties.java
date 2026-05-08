package io.github.valossa515.pricetracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.marketplace")
public record MarketplaceProperties(List<String> allowedHosts) {
}
