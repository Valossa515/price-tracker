package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.alert.dto.AlertResponse;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GetAlertByIdHandler implements QueryHandler<GetAlertByIdQuery, AlertResponse> {

    private final AlertRepository repository;

    @Override
    @Transactional(readOnly = true)
    public AlertResponse handle(GetAlertByIdQuery query) {
        return repository.findById(query.alertId())
                .map(AlertResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
