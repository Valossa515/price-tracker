package io.github.valossa515.pricetracker.marketplace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MercadoLivreClient implements MarketplaceClient {

    private static final Pattern CATALOG_ID = Pattern.compile("/p/MLB-?(\\d+)");
    private static final Pattern ITEM_ID = Pattern.compile("MLB-?(\\d+)");
    private static final String HOST = "mercadolivre.com.br";

    private final RestClient restClient;
    private final MercadoLivreTokenProvider tokenProvider;

    public MercadoLivreClient(
            @Qualifier("mlRestClient") RestClient restClient,
            MercadoLivreTokenProvider tokenProvider) {
        this.restClient = restClient;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public String hostKey() {
        return HOST;
    }

    @Override
    public Optional<ProductInfo> fetchProduct(String url) {
        Matcher catMatch = CATALOG_ID.matcher(url);
        if (catMatch.find()) {
            return fetchCatalog("MLB" + catMatch.group(1));
        }
        Matcher itemMatch = ITEM_ID.matcher(url);
        if (itemMatch.find()) {
            return fetchItem("MLB" + itemMatch.group(1));
        }
        log.warn("No MLB id found in url");
        return Optional.empty();
    }

    @Override
    public List<ProductSearchResult> search(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        int safeLimit = Math.max(1, Math.min(limit, 20));
        try {
            String token = tokenProvider.getToken();
            SearchResponse resp = restClient.get()
                    .uri(uri -> uri.path("/sites/MLB/search")
                            .queryParam("q", query)
                            .queryParam("limit", safeLimit)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(SearchResponse.class);
            if (resp == null || resp.results == null) return List.of();
            return resp.results.stream()
                    .filter(r -> r.title != null && r.price != null)
                    .map(r -> new ProductSearchResult(
                            HOST,
                            r.title,
                            BigDecimal.valueOf(r.price),
                            r.permalink != null ? r.permalink : "https://www.mercadolivre.com.br/p/" + r.id))
                    .toList();
        } catch (RestClientException e) {
            log.warn("ML search failed for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    private Optional<ProductInfo> fetchCatalog(String catalogId) {
        try {
            String token = tokenProvider.getToken();
            CatalogProduct catalog = restClient.get()
                    .uri("/products/{id}", catalogId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(CatalogProduct.class);
            CatalogItems offers = restClient.get()
                    .uri("/products/{id}/items", catalogId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(CatalogItems.class);

            if (catalog == null || offers == null
                    || offers.results == null || offers.results.isEmpty()) {
                return Optional.empty();
            }
            List<CatalogOffer> available = offers.results.stream()
                    .filter(o -> o.price != null)
                    .filter(CatalogOffer::isAvailable)
                    .toList();
            boolean isAvailable = !available.isEmpty();
            List<CatalogOffer> source = isAvailable ? available : offers.results;
            Optional<BigDecimal> lowest = source.stream()
                    .map(CatalogOffer::price)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::valueOf)
                    .min(Comparator.naturalOrder());
            return lowest.map(price -> new ProductInfo(catalog.name(), price, isAvailable));
        } catch (RestClientException e) {
            log.warn("ML catalog fetch failed for {}: {}", catalogId, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ProductInfo> fetchItem(String itemId) {
        try {
            String token = tokenProvider.getToken();
            MlItem item = restClient.get()
                    .uri("/items/{id}", itemId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(MlItem.class);
            if (item == null || item.price == null) {
                return Optional.empty();
            }
            return Optional.of(new ProductInfo(item.title, BigDecimal.valueOf(item.price), item.isAvailable()));
        } catch (RestClientException e) {
            log.warn("ML item fetch failed for {}: {}", itemId, e.getMessage());
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MlItem(String title, Double price, Integer available_quantity, String status) {
        boolean isAvailable() {
            boolean active = status == null || "active".equalsIgnoreCase(status);
            boolean hasStock = available_quantity == null || available_quantity > 0;
            return active && hasStock;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CatalogProduct(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CatalogItems(List<CatalogOffer> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CatalogOffer(Double price, Integer available_quantity, String status) {
        boolean isAvailable() {
            boolean active = status == null || "active".equalsIgnoreCase(status);
            boolean hasStock = available_quantity == null || available_quantity > 0;
            return active && hasStock;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResponse(List<SearchHit> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchHit(String id, String title, Double price, String permalink) {
    }
}
