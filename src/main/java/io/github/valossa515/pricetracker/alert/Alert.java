package io.github.valossa515.pricetracker.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
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
@Table(name = "alert")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "user_email", length = 320)
    private String userEmail;

    @Column(name = "product_url", nullable = false, length = 2048)
    private String productUrl;

    @Column(name = "product_name", length = 500)
    private String productName;

    @Column(name = "target_price", precision = 12, scale = 2)
    private BigDecimal targetPrice;

    @Column(name = "last_observed_price", precision = 12, scale = 2)
    private BigDecimal lastObservedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "drop_window_days")
    private Integer dropWindowDays;

    @Column(name = "drop_percent", precision = 5, scale = 2)
    private BigDecimal dropPercent;

    @Column(name = "last_observed_available")
    private Boolean lastObservedAvailable;

    @Column(name = "real_discount_flag")
    private Boolean realDiscountFlag;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "webhook_url", length = 2048)
    private String webhookUrl;

    @Column(name = "webhook_secret", length = 128)
    private String webhookSecret;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = AlertStatus.ACTIVE;
        }
        if (alertType == null) {
            alertType = AlertType.PRICE_BELOW_TARGET;
        }
    }
}
