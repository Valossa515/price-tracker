package io.github.valossa515.pricetracker.security;

import io.github.valossa515.pricetracker.config.MarketplaceProperties;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
public class UrlAllowlistService {

    private final List<String> allowedHosts;

    public UrlAllowlistService(MarketplaceProperties properties) {
        this.allowedHosts = properties.allowedHosts() == null
                ? List.of()
                : List.copyOf(properties.allowedHosts());
    }

    public boolean isAllowed(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }
        String hostLower = host.toLowerCase();
        return allowedHosts.stream().anyMatch(allowed -> {
            String allowedLower = allowed.toLowerCase();
            return hostLower.equals(allowedLower)
                    || hostLower.endsWith("." + allowedLower);
        });
    }
}
