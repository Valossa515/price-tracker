package io.github.valossa515.pricetracker.pricecheck;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceCheckHistoryRepository extends JpaRepository<PriceCheckHistory, Long> {

    List<PriceCheckHistory> findByAlertIdAndObservedAtGreaterThanEqualOrderByObservedAtDesc(
            UUID alertId, Instant since, Pageable pageable);

    Optional<PriceCheckHistory> findFirstByAlertIdAndObservedAtLessThanEqualOrderByObservedAtDesc(
            UUID alertId, Instant cutoff);

    @Query("""
            SELECT MIN(h.observedPrice) AS minPrice,
                   MAX(h.observedPrice) AS maxPrice,
                   AVG(h.observedPrice) AS avgPrice,
                   COUNT(h)             AS samples
              FROM PriceCheckHistory h
             WHERE h.alertId = :alertId
               AND h.observedAt >= :since
            """)
    PriceStats aggregateSince(@Param("alertId") UUID alertId, @Param("since") Instant since);

    interface PriceStats {
        BigDecimal getMinPrice();
        BigDecimal getMaxPrice();
        BigDecimal getAvgPrice();
        Long getSamples();
    }
}
