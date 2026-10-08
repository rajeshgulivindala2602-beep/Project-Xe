package com.xe.ratealerts.alerts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Superset of the old stub fields (id, pair, threshold, direction, triggered).
 * currentRate is null if the rate could not be fetched.
 */
public record AlertView(
        UUID id,
        String pair,
        BigDecimal threshold,
        Direction direction,
        boolean triggered,
        Instant createdAt,
        Instant triggeredAt,
        BigDecimal currentRate) {

    public static AlertView of(Alert alert, BigDecimal currentRate) {
        return new AlertView(
                alert.id(),
                alert.pair(),
                alert.threshold(),
                alert.direction(),
                alert.isTriggered(),
                alert.createdAt(),
                alert.triggeredAt(),
                currentRate);
    }
}
