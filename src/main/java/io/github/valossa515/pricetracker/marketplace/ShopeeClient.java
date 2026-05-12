package io.github.valossa515.pricetracker.marketplace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches product info from Shopee BR via Affiliate Open API (GraphQL).
 * <p>
 * Supported URL formats:
 * <ul>
 *   <li>{@code https://shopee.com.br/<slug>-i.<shopId>.<itemId>}</li>
 *   <li>{@code https://shopee.com.br/product/<shopId>/<itemId>}</li>
 * </ul>
 * Short links (s.shopee.com.br) are not resolved here; the user must paste the canonical URL.
 */
@Slf4j
@Component
public class ShopeeClient implements MarketplaceClient {

    private static final String HOST = "shopee.com.br";
    private static final Pattern URL_I_PATTERN =
            Pattern.compile("-i\\.(\\d+)\\.(\\d+)");
    private static final Pattern URL_PRODUCT_PATTERN =
            Pattern.compile("/product/(\\d+)/(\\d+)");

    private static final String QUERY = """
            query Fetch($itemId: Int64!, $shopId: Int64!) {
              productOfferV2(itemId: $itemId, shopId: $shopId) {
                nodes {
                  itemId
                  shopId
                  productName
                  price
                  priceMin
                  priceMax
                }
              }
            }
            """;

    private final RestClient restClient;
    private final ShopeeSignatureSigner signer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ShopeeClient(
            @Qualifier("shopeeRestClient") RestClient restClient,
            ShopeeSignatureSigner signer) {
        this.restClient = restClient;
        this.signer = signer;
    }

    @Override
    public String hostKey() {
        return HOST;
    }

    @Override
    public Optional<ProductInfo> fetchProduct(String url) {
        if (!signer.isConfigured()) {
            log.warn("Shopee API credentials not configured; skipping {}", url);
            return Optional.empty();
        }

        Optional<long[]> ids = extractIds(url);
        if (ids.isEmpty()) {
            log.warn("No Shopee shopId/itemId found in url");
            return Optional.empty();
        }
        long shopId = ids.get()[0];
        long itemId = ids.get()[1];

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "query", QUERY,
                    "variables", Map.of("itemId", itemId, "shopId", shopId)
            ));
            String auth = signer.authorizationHeader(payload);

            GraphQLResponse response = restClient.post()
                    .uri("")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .body(payload)
                    .retrieve()
                    .body(GraphQLResponse.class);

            if (response == null) {
                return Optional.empty();
            }
            if (response.errors != null && !response.errors.isEmpty()) {
                log.warn("Shopee GraphQL errors: {}", response.errors);
                return Optional.empty();
            }
            if (response.data == null
                    || response.data.productOfferV2 == null
                    || response.data.productOfferV2.nodes == null
                    || response.data.productOfferV2.nodes.isEmpty()) {
                return Optional.empty();
            }
            ProductOfferNode node = response.data.productOfferV2.nodes.get(0);
            BigDecimal price = pickPrice(node);
            if (price == null || node.productName == null) {
                return Optional.empty();
            }
            return Optional.of(new ProductInfo(node.productName, price, true));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize Shopee GraphQL payload: {}", e.getMessage());
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("Shopee fetch failed for shop={}, item={}: {}", shopId, itemId, e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<long[]> extractIds(String url) {
        Matcher m = URL_I_PATTERN.matcher(url);
        if (m.find()) {
            return Optional.of(new long[]{Long.parseLong(m.group(1)), Long.parseLong(m.group(2))});
        }
        Matcher m2 = URL_PRODUCT_PATTERN.matcher(url);
        if (m2.find()) {
            return Optional.of(new long[]{Long.parseLong(m2.group(1)), Long.parseLong(m2.group(2))});
        }
        return Optional.empty();
    }

    private static BigDecimal pickPrice(ProductOfferNode node) {
        if (node.priceMin != null) return node.priceMin;
        if (node.price != null) return node.price;
        if (node.priceMax != null) return node.priceMax;
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GraphQLResponse(GraphQLData data, List<Object> errors) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GraphQLData(ProductOfferResponse productOfferV2) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProductOfferResponse(List<ProductOfferNode> nodes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProductOfferNode(
            Long itemId,
            Long shopId,
            String productName,
            BigDecimal price,
            BigDecimal priceMin,
            BigDecimal priceMax
    ) {
    }
}
