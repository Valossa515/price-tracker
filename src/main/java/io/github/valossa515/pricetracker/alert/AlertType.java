package io.github.valossa515.pricetracker.alert;

public enum AlertType {
    /** Triggers when current price &lt;= targetPrice. */
    PRICE_BELOW_TARGET,
    /** Triggers when current price &lt;= avg(last 30d) * (1 - discountPercent/100). */
    PERCENT_DISCOUNT,
    /** Triggers when current price &lt;= price(N days ago) * (1 - dropPercent/100). */
    PRICE_DROP,
    /** Triggers when product transitions from unavailable to available. */
    BACK_IN_STOCK
}
