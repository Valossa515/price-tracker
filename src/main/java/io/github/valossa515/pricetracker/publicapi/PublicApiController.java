package io.github.valossa515.pricetracker.publicapi;

import io.github.valossa515.pricetracker.alert.Alert;
import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.alert.cqrs.CreateAlertCommand;
import io.github.valossa515.pricetracker.alert.cqrs.DeleteAlertCommand;
import io.github.valossa515.pricetracker.alert.cqrs.GetAlertHistoryQuery;
import io.github.valossa515.pricetracker.alert.dto.AlertResponse;
import io.github.valossa515.pricetracker.alert.dto.PriceHistoryPointResponse;
import io.github.valossa515.pricetracker.marketplace.ProductInfo;
import io.github.valossa515.pricetracker.marketplace.cqrs.FetchProductPriceQuery;
import io.github.valossa515.pricetracker.publicapi.dto.PublicAlertResponse;
import io.github.valossa515.pricetracker.publicapi.dto.PublicCreateAlertRequest;
import io.github.valossa515.pricetracker.publicapi.dto.PublicPriceResponse;
import io.github.valossa515.pricetracker.security.UrlAllowlistService;
import io.github.valossa515.spring_courier.core.Courier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * External API consumed by other services. Authentication: Bearer JWT issued by
 * Cognito with scope {@code app.publicapi.required-scope} (default {@code external-api}).
 * Calls are rate-limited per JWT subject by {@link PublicApiRateLimitFilter}.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Validated
@Tag(name = "Public API", description = "External endpoints for partner services. Requires OAuth2 scope `external-api` and is rate-limited per JWT subject.")
public class PublicApiController {

    private final Courier courier;
    private final AlertRepository alertRepository;
    private final UrlAllowlistService urlAllowlist;

    @Operation(summary = "Fetch the current price of a supported marketplace product by URL.")
    @GetMapping("/products/price")
    public PublicPriceResponse currentPrice(@RequestParam("url") @NotBlank String url) {
        if (!urlAllowlist.isAllowed(url)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "URL is not from a supported marketplace");
        }
        ProductInfo info = courier.send(new FetchProductPriceQuery(url)).getData();
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Price not available");
        }
        return new PublicPriceResponse(url, info.name(), info.currentPrice(),
                info.available(), Instant.now());
    }

    @Operation(summary = "Create an alert. Optionally register a webhook for trigger notifications.")
    @PostMapping("/alerts")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicAlertResponse createAlert(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PublicCreateAlertRequest req) {

        if (!urlAllowlist.isAllowed(req.productUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "URL is not from a supported marketplace");
        }

        String email = req.ownerEmail() != null && !req.ownerEmail().isBlank()
                ? req.ownerEmail()
                : jwt.getClaimAsString("email");
        if ((email == null || email.isBlank())
                && (req.webhookUrl() == null || req.webhookUrl().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide ownerEmail or webhookUrl so the alert can deliver notifications");
        }

        CreateAlertCommand cmd = new CreateAlertCommand(
                jwt.getSubject(),
                email,
                req.productUrl(),
                req.productName(),
                req.resolvedType(),
                req.targetPrice(),
                req.discountPercent(),
                req.dropWindowDays(),
                req.dropPercent(),
                req.webhookUrl(),
                req.webhookSecret()
        );
        AlertResponse created = courier.send(cmd).getDataOrThrow();
        // Re-read to expose webhookConfigured flag from the persisted entity.
        return alertRepository.findById(created.id())
                .map(PublicAlertResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @GetMapping("/alerts")
    @Operation(summary = "List alerts owned by the authenticated subject.")
    public List<PublicAlertResponse> listAlerts(@AuthenticationPrincipal Jwt jwt) {
        return alertRepository.findByUserId(jwt.getSubject()).stream()
                .map(PublicAlertResponse::from)
                .toList();
    }

    @GetMapping("/alerts/{id}")
    @Operation(summary = "Get a single alert owned by the authenticated subject.")
    public PublicAlertResponse getAlert(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        Alert alert = loadOwned(id, jwt.getSubject());
        return PublicAlertResponse.from(alert);
    }

    @GetMapping("/alerts/{id}/history")
    @Operation(summary = "Return observed price points for the alert within the last `days`.")
    public List<PriceHistoryPointResponse> alertHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "500") int limit) {
        // GetAlertHistoryQuery enforces ownership through OwnershipPipelineBehavior.
        return courier.send(new GetAlertHistoryQuery(id, jwt.getSubject(), days, limit))
                .getDataOrThrow();
    }

    @DeleteMapping("/alerts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an alert owned by the authenticated subject.")
    public void deleteAlert(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        courier.send(new DeleteAlertCommand(id, jwt.getSubject())).getDataOrThrow();
    }

    private Alert loadOwned(UUID alertId, String userId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!alert.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return alert;
    }
}
