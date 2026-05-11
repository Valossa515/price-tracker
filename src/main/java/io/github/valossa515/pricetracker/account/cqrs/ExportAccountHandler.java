package io.github.valossa515.pricetracker.account.cqrs;

import io.github.valossa515.pricetracker.account.dto.AccountExportResponse;
import io.github.valossa515.pricetracker.alert.Alert;
import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.consent.UserConsent;
import io.github.valossa515.pricetracker.consent.UserConsentRepository;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ExportAccountHandler implements QueryHandler<ExportAccountQuery, AccountExportResponse> {

    private static final String FORMAT_VERSION = "1";

    private final AlertRepository alerts;
    private final UserConsentRepository consents;

    @Override
    @Transactional(readOnly = true)
    public AccountExportResponse handle(ExportAccountQuery query) {
        String userId = query.userId();

        List<AccountExportResponse.Alert> alertDtos = alerts.findByUserId(userId).stream()
                .map(ExportAccountHandler::toDto)
                .toList();

        List<AccountExportResponse.Consent> consentDtos = consents.findByUserId(userId).stream()
                .map(ExportAccountHandler::toDto)
                .toList();

        return new AccountExportResponse(
                Instant.now(),
                FORMAT_VERSION,
                new AccountExportResponse.Account(userId, query.email()),
                alertDtos,
                consentDtos
        );
    }

    private static AccountExportResponse.Alert toDto(Alert a) {
        return new AccountExportResponse.Alert(
                Objects.toString(a.getId(), null),
                a.getProductUrl(),
                a.getProductName(),
                toPlainString(a.getTargetPrice()),
                toPlainString(a.getLastObservedPrice()),
                a.getStatus() != null ? a.getStatus().name() : null,
                a.getCreatedAt(),
                a.getLastCheckedAt()
        );
    }

    private static AccountExportResponse.Consent toDto(UserConsent c) {
        return new AccountExportResponse.Consent(
                Objects.toString(c.getId(), null),
                c.getDocumentType() != null ? c.getDocumentType().name() : null,
                c.getVersion(),
                c.getAcceptedAt(),
                c.getIpAddress(),
                c.getUserAgent()
        );
    }

    private static String toPlainString(BigDecimal v) {
        return v == null ? null : v.toPlainString();
    }
}
