package io.github.valossa515.pricetracker.account;

import io.github.valossa515.pricetracker.account.cqrs.DeleteAccountCommand;
import io.github.valossa515.spring_courier.core.Courier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class AccountController {

    private final Courier courier;

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT missing sub claim");
        }
        courier.send(new DeleteAccountCommand(sub)).getDataOrThrow();
    }
}
