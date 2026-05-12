package io.github.valossa515.pricetracker.pricecheck;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_check_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceCheckHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Column(name = "observed_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal observedPrice;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "available")
    private Boolean available;

    @PrePersist
    void prePersist() {
        if (observedAt == null) {
            observedAt = Instant.now();
        }
    }
}
