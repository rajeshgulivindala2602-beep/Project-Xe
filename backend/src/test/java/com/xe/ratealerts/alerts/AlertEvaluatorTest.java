package com.xe.ratealerts.alerts;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AlertEvaluatorTest {

    private final AlertEvaluator evaluator = new AlertEvaluator();
    private final BigDecimal threshold = new BigDecimal("1.84");

    @Test
    void aboveUsesStrictComparison() {
        assertThat(evaluator.isCrossed(Direction.ABOVE, threshold, new BigDecimal("1.85"))).isTrue();
        assertThat(evaluator.isCrossed(Direction.ABOVE, threshold, new BigDecimal("1.84"))).isFalse();
        assertThat(evaluator.isCrossed(Direction.ABOVE, threshold, new BigDecimal("1.83"))).isFalse();
    }

    @Test
    void belowUsesStrictComparison() {
        assertThat(evaluator.isCrossed(Direction.BELOW, threshold, new BigDecimal("1.85"))).isFalse();
        assertThat(evaluator.isCrossed(Direction.BELOW, threshold, new BigDecimal("1.84"))).isFalse();
        assertThat(evaluator.isCrossed(Direction.BELOW, threshold, new BigDecimal("1.83"))).isTrue();
    }

    @Test
    void treatsDifferentBigDecimalScalesAsEqual() {
        assertThat(evaluator.isCrossed(
                Direction.ABOVE,
                new BigDecimal("1.84"),
                new BigDecimal("1.8400"))).isFalse();
        assertThat(evaluator.isCrossed(
                Direction.BELOW,
                new BigDecimal("1.84"),
                new BigDecimal("1.8400"))).isFalse();
    }

    @Test
    void marksCrossedAlertTriggeredAtNow() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        Alert alert = Alert.create("USD/CAD", threshold, Direction.ABOVE, now.minusSeconds(10));

        Alert evaluated = evaluator.evaluate(alert, new BigDecimal("1.85"), now);

        assertThat(evaluated.isTriggered()).isTrue();
        assertThat(evaluated.triggeredAt()).isEqualTo(now);
        assertThat(evaluated.id()).isEqualTo(alert.id());
    }

    @Test
    void leavesUncrossedAlertUnchanged() {
        Alert alert = Alert.create(
                "USD/CAD", threshold, Direction.ABOVE, Instant.parse("2025-01-01T00:00:00Z"));

        assertThat(evaluator.evaluate(alert, new BigDecimal("1.83"), Instant.now())).isEqualTo(alert);
    }

    @Test
    void triggeredAlertKeepsOriginalTimeWhenRateReverses() {
        Instant originalTrigger = Instant.parse("2025-01-01T00:00:00Z");
        Alert alert = Alert.create("USD/CAD", threshold, Direction.ABOVE, originalTrigger.minusSeconds(10))
                .triggeredAt(originalTrigger);

        Alert evaluated = evaluator.evaluate(alert, new BigDecimal("1.83"), originalTrigger.plusSeconds(10));

        assertThat(evaluated).isEqualTo(alert);
        assertThat(evaluated.triggeredAt()).isEqualTo(originalTrigger);
    }
}
