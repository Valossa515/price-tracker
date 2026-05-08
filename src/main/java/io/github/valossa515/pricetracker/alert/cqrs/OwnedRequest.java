package io.github.valossa515.pricetracker.alert.cqrs;

import java.util.UUID;

/**
 * Marker interface for requests that operate on a specific Alert owned by a user.
 * Triggers the OwnershipPipelineBehavior, which verifies that the alertId
 * exists and that the userId matches the alert's owner before the handler runs.
 */
public interface OwnedRequest {
    UUID alertId();
    String userId();
}
