package io.github.valossa515.pricetracker.alert.pipeline;

import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.alert.cqrs.OwnedRequest;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior.Next;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global pipeline behavior that enforces ownership for any request implementing
 * {@link OwnedRequest}. Runs before the handler — if the alertId doesn't exist
 * returns 404; if the requester's userId differs from the alert's owner, returns 403
 * and emits a WARN log (UUIDs only — no PII).
 *
 * Other requests (e.g. CreateAlertCommand, ListMyAlertsQuery) pass through untouched.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class OwnershipPipelineBehavior implements PipelineBehavior<IRequest<Object>, Object> {

    private final AlertRepository repository;

    @Override
    public Object handle(IRequest<Object> request, Next<Object> next) {
        if (request instanceof OwnedRequest owned) {
            var alert = repository.findById(owned.alertId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

            if (!alert.getUserId().equals(owned.userId())) {
                log.warn("Forbidden: user {} attempted to access alert {} owned by {}",
                        owned.userId(), owned.alertId(), alert.getUserId());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        return next.invoke();
    }
}
