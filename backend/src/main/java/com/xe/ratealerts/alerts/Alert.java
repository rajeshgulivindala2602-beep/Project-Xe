package com.xe.ratealerts.alerts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * triggeredAt is null until fired and is never cleared; the alert latches.
 */
public record Alert(
        UUID id,
        String pair,
        BigDecimal threshold,
        Direction direction,
        Instant createdAt,
        Instant triggeredAt) {

    public static Alert create(String pair, BigDecimal threshold, Direction direction, Instant now) {
        return new Alert(UUID.randomUUID(), pair, threshold, direction, now, null);
    }

    public boolean isTriggered() {
        return triggeredAt != null;
    }

    public Alert triggeredAt(Instant when) {
        return new Alert(id, pair, threshold, direction, createdAt, when);
    }
}
