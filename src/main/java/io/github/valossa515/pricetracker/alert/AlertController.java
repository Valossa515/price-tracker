package io.github.valossa515.pricetracker.alert;

import io.github.valossa515.pricetracker.alert.cqrs.CreateAlertCommand;
import io.github.valossa515.pricetracker.alert.cqrs.DeleteAlertCommand;
import io.github.valossa515.pricetracker.alert.cqrs.GetAlertByIdQuery;
import io.github.valossa515.pricetracker.alert.cqrs.ListMyAlertsQuery;
import io.github.valossa515.pricetracker.alert.cqrs.UpdateAlertTargetPriceCommand;
import io.github.valossa515.pricetracker.alert.dto.AlertResponse;
import io.github.valossa515.pricetracker.alert.dto.CreateAlertRequest;
import io.github.valossa515.pricetracker.alert.dto.UpdateAlertTargetPriceRequest;
import io.github.valossa515.pricetracker.pricecheck.cqrs.CheckAllActiveAlertsCommand;
import io.github.valossa515.pricetracker.security.UrlAllowlistService;
import io.github.valossa515.spring_courier.core.Courier;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final Courier courier;
    private final UrlAllowlistService urlAllowlist;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAlertRequest req) {

        if (!urlAllowlist.isAllowed(req.productUrl())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "URL is not from a supported marketplace"
            );
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "JWT must contain an email claim"
            );
        }

        CreateAlertCommand cmd = new CreateAlertCommand(
                jwt.getSubject(),
                email,
                req.productUrl(),
                req.productName(),
                req.targetPrice()
        );
        return courier.send(cmd).getDataOrThrow();
    }

    @GetMapping
    public List<AlertResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return courier.send(new ListMyAlertsQuery(jwt.getSubject())).getDataOrThrow();
    }

    @GetMapping("/{id}")
    public AlertResponse getById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return courier.send(new GetAlertByIdQuery(id, jwt.getSubject())).getDataOrThrow();
    }

    @PatchMapping("/{id}")
    public AlertResponse updateTargetPrice(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAlertTargetPriceRequest req) {
        UpdateAlertTargetPriceCommand cmd =
                new UpdateAlertTargetPriceCommand(id, jwt.getSubject(), req.targetPrice());
        return courier.send(cmd).getDataOrThrow();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        courier.send(new DeleteAlertCommand(id, jwt.getSubject())).getDataOrThrow();
    }

    @PostMapping("/check-now")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> triggerCheck() {
        Integer processed = courier.send(new CheckAllActiveAlertsCommand()).getDataOrThrow();
        return Map.of("processed", processed);
    }
}
