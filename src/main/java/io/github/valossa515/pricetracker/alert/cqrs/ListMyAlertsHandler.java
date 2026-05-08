package io.github.valossa515.pricetracker.alert.cqrs;

import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.alert.dto.AlertResponse;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyAlertsHandler implements QueryHandler<ListMyAlertsQuery, List<AlertResponse>> {

    private final AlertRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponse> handle(ListMyAlertsQuery query) {
        return repository.findByUserId(query.userId()).stream()
                .map(AlertResponse::from)
                .toList();
    }
}
