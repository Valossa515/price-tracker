package io.github.valossa515.pricetracker.consent.cqrs;

import io.github.valossa515.pricetracker.consent.ConsentDocumentsProperties;
import io.github.valossa515.pricetracker.consent.UserConsent;
import io.github.valossa515.pricetracker.consent.UserConsentRepository;
import io.github.valossa515.pricetracker.consent.dto.AcceptConsentResponse;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AcceptConsentHandler implements CommandHandler<AcceptConsentCommand, AcceptConsentResponse> {

    private final UserConsentRepository repository;
    private final ConsentDocumentsProperties properties;

    @Override
    @Transactional
    public AcceptConsentResponse handle(AcceptConsentCommand cmd) {
        ConsentDocumentsProperties.DocumentSpec spec = properties.getDocuments().get(cmd.documentType());
        if (spec == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown document type: " + cmd.documentType());
        }
        if (!spec.getVersion().equals(cmd.version())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Document version mismatch. Expected " + spec.getVersion()
                            + " but got " + cmd.version());
        }

        UserConsent consent = UserConsent.builder()
                .userId(cmd.userId())
                .documentType(cmd.documentType())
                .version(cmd.version())
                .ipAddress(cmd.ipAddress())
                .userAgent(cmd.userAgent())
                .build();
        UserConsent saved = repository.save(consent);
        return new AcceptConsentResponse(
                saved.getId(),
                saved.getDocumentType(),
                saved.getVersion(),
                saved.getAcceptedAt());
    }
}
