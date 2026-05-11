package io.github.valossa515.pricetracker.consent;

import io.github.valossa515.pricetracker.consent.cqrs.AcceptConsentCommand;
import io.github.valossa515.pricetracker.consent.cqrs.GetRequiredConsentsQuery;
import io.github.valossa515.pricetracker.consent.dto.AcceptConsentRequest;
import io.github.valossa515.pricetracker.consent.dto.AcceptConsentResponse;
import io.github.valossa515.pricetracker.consent.dto.PendingConsentResponse;
import io.github.valossa515.spring_courier.core.Courier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final Courier courier;

    @GetMapping("/required")
    public List<PendingConsentResponse> required(@AuthenticationPrincipal Jwt jwt) {
        String userId = requireSub(jwt);
        return courier.send(new GetRequiredConsentsQuery(userId)).getDataOrThrow();
    }

    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public AcceptConsentResponse accept(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody AcceptConsentRequest req,
                                        HttpServletRequest http) {
        String userId = requireSub(jwt);
        return courier.send(new AcceptConsentCommand(
                userId,
                req.documentType(),
                req.version(),
                clientIp(http),
                http.getHeader("User-Agent"))).getDataOrThrow();
    }

    private static String requireSub(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT missing sub claim");
        }
        return sub;
    }

    private static String clientIp(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma >= 0 ? xff.substring(0, comma) : xff).trim();
        }
        return http.getRemoteAddr();
    }
}
