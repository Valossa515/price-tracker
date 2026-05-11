package io.github.valossa515.pricetracker.account;

import io.github.valossa515.pricetracker.account.cqrs.DeleteAccountCommand;
import io.github.valossa515.pricetracker.account.cqrs.ExportAccountQuery;
import io.github.valossa515.pricetracker.account.dto.AccountExportResponse;
import io.github.valossa515.spring_courier.core.Courier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class AccountController {

    private final Courier courier;

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt) {
        String sub = requireSub(jwt);
        courier.send(new DeleteAccountCommand(sub)).getDataOrThrow();
    }

    @GetMapping("/export")
    public ResponseEntity<AccountExportResponse> export(@AuthenticationPrincipal Jwt jwt) {
        String sub = requireSub(jwt);
        String email = jwt.getClaimAsString("email");
        AccountExportResponse body = courier.send(new ExportAccountQuery(sub, email)).getDataOrThrow();
        String filename = "littlepricetracker-export-" + Instant.now().getEpochSecond() + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

    private static String requireSub(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT missing sub claim");
        }
        return sub;
    }
}
