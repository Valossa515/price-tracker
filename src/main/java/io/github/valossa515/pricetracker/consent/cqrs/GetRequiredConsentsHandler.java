package io.github.valossa515.pricetracker.consent.cqrs;

import io.github.valossa515.pricetracker.consent.ConsentDocumentType;
import io.github.valossa515.pricetracker.consent.ConsentDocumentsProperties;
import io.github.valossa515.pricetracker.consent.UserConsent;
import io.github.valossa515.pricetracker.consent.UserConsentRepository;
import io.github.valossa515.pricetracker.consent.dto.PendingConsentResponse;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetRequiredConsentsHandler
        implements QueryHandler<GetRequiredConsentsQuery, List<PendingConsentResponse>> {

    private final UserConsentRepository repository;
    private final ConsentDocumentsProperties properties;

    @Override
    @Transactional(readOnly = true)
    public List<PendingConsentResponse> handle(GetRequiredConsentsQuery query) {
        List<PendingConsentResponse> pending = new ArrayList<>();
        for (var entry : properties.getDocuments().entrySet()) {
            ConsentDocumentType type = entry.getKey();
            ConsentDocumentsProperties.DocumentSpec spec = entry.getValue();
            Optional<UserConsent> latest = repository.findLatest(query.userId(), type);
            boolean accepted = latest.isPresent()
                    && latest.get().getVersion().equals(spec.getVersion());
            if (!accepted) {
                pending.add(new PendingConsentResponse(
                        type, spec.getVersion(), spec.getTitle(), spec.getUrl()));
            }
        }
        return pending;
    }
}
