package com.xe.ratealerts.alerts;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Alerts fire only on strict threshold crossings; equal values do not fire.
 * Once fired, an alert retains its original trigger time even if the rate reverses.
 */
@Component
public class AlertEvaluator {

    public boolean isCrossed(Direction direction, BigDecimal threshold, BigDecimal rate) {
        int comparison = rate.compareTo(threshold);
        return switch (direction) {
            case ABOVE -> comparison > 0;
            case BELOW -> comparison < 0;
        };
    }

    public Alert evaluate(Alert alert, BigDecimal currentRate, Instant now) {
        if (alert.isTriggered() || !isCrossed(alert.direction(), alert.threshold(), currentRate)) {
            return alert;
        }
        return alert.triggeredAt(now);
    }
}
