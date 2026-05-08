package io.github.valossa515.pricetracker.marketplace;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Component
public class MarketplaceResolver {

    private final List<MarketplaceClient> clients;

    public MarketplaceResolver(List<MarketplaceClient> clients) {
        this.clients = clients;
    }

    public Optional<MarketplaceClient> resolve(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return Optional.empty();
            String hostLower = host.toLowerCase();
            return clients.stream()
                    .filter(c -> hostLower.equals(c.hostKey())
                            || hostLower.endsWith("." + c.hostKey()))
                    .findFirst();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
