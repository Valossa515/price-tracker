package io.github.valossa515.pricetracker.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByUserId(String userId);

    List<Alert> findByStatus(AlertStatus status);
}
