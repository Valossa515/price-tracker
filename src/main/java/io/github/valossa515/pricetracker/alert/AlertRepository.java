package io.github.valossa515.pricetracker.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByUserId(String userId);

    List<Alert> findByStatus(AlertStatus status);

    @Modifying
    @Query("DELETE FROM Alert a WHERE a.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}
