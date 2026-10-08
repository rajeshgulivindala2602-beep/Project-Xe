package com.xe.ratealerts.alerts;

import com.xe.ratealerts.rates.Rate;
import com.xe.ratealerts.rates.RateProvider;
import com.xe.ratealerts.rates.RateService;
import com.xe.ratealerts.rates.RateUnavailableException;
import com.xe.ratealerts.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertServiceTest {

    private static final Instant START = Instant.parse("2025-01-01T00:00:00Z");

    @Test
    void rejectsUnsupportedPairWithSupportedPairGuidance() {
        Fixture fixture = new Fixture();

        assertThatThrownBy(() -> fixture.service.create(
                "XXX/YYY", new BigDecimal("1.0"), Direction.ABOVE))
                .isInstanceOf(InvalidAlertException.class)
                .hasMessageContaining("XXX/YYY")
                .hasMessageContaining("USD/CAD");
    }

    @Test
    void rejectsNullPair() {
        Fixture fixture = new Fixture();

        assertThatThrownBy(() -> fixture.service.create(null, BigDecimal.ONE, Direction.ABOVE))
                .isInstanceOf(InvalidAlertException.class)
                .hasMessage("Unknown pair 'null'. Supported pairs: USD/CAD, GBP/USD, EUR/USD.");
    }

    @Test
    void rejectsNullZeroAndNegativeThresholds() {
        Fixture fixture = new Fixture();

        assertThatThrownBy(() -> fixture.service.create("USD/CAD", null, Direction.ABOVE))
                .isInstanceOf(InvalidAlertException.class)
                .hasMessage("Threshold must be a number greater than 0.");
        assertThatThrownBy(() -> fixture.service.create(
                "USD/CAD", BigDecimal.ZERO, Direction.ABOVE))
                .isInstanceOf(InvalidAlertException.class)
                .hasMessage("Threshold must be a number greater than 0.");
        assertThatThrownBy(() -> fixture.service.create(
                "USD/CAD", new BigDecimal("-0.1"), Direction.ABOVE))
                .isInstanceOf(InvalidAlertException.class)
                .hasMessage("Threshold must be a number greater than 0.");
    }

    @Test
    void rejectsNullDirection() {
        Fixture fixture = new Fixture();

        assertThatThrownBy(() -> fixture.service.create("USD/CAD", BigDecimal.ONE, null))
                .isInstanceOf(InvalidAlertException.class)
                .hasMessage("Direction must be 'above' or 'below'.");
    }

    @Test
    void createsUntriggeredAlertWhenRateHasNotCrossedThreshold() {
        Fixture fixture = new Fixture();
        fixture.rates.put("USD/CAD", new BigDecimal("1.40"));

        AlertView created = fixture.service.create(
                "USD/CAD", new BigDecimal("2.00"), Direction.ABOVE);

        assertThat(created.triggered()).isFalse();
        assertThat(created.createdAt()).isEqualTo(START);
        assertThat(created.currentRate()).isEqualByComparingTo("1.40");
        assertThat(fixture.service.list()).containsExactly(created);
    }

    @Test
    void immediatelyTriggersAlertAlreadyAcrossThreshold() {
        Fixture fixture = new Fixture();
        fixture.rates.put("USD/CAD", new BigDecimal("1.40"));

        AlertView created = fixture.service.create(
                "USD/CAD", new BigDecimal("1.30"), Direction.ABOVE);

        assertThat(created.triggered()).isTrue();
        assertThat(created.triggeredAt()).isEqualTo(START);
    }

    @Test
    void listTriggersOnCrossingAndKeepsOriginalTriggerTimeAfterReversal() {
        Fixture fixture = new Fixture();
        fixture.rates.put("USD/CAD", new BigDecimal("1.20"));
        AlertView created = fixture.service.create(
                "USD/CAD", new BigDecimal("1.30"), Direction.ABOVE);
        fixture.clock.advance(Duration.ofSeconds(5));
        fixture.rates.put("USD/CAD", new BigDecimal("1.40"));

        AlertView fired = fixture.service.list().getFirst();
        fixture.clock.advance(Duration.ofSeconds(5));
        fixture.rates.put("USD/CAD", new BigDecimal("1.10"));
        AlertView afterReversal = fixture.service.list().getFirst();

        assertThat(created.triggered()).isFalse();
        assertThat(fired.triggered()).isTrue();
        assertThat(fired.triggeredAt()).isEqualTo(START.plusSeconds(5));
        assertThat(afterReversal.triggered()).isTrue();
        assertThat(afterReversal.triggeredAt()).isEqualTo(fired.triggeredAt());
    }

    @Test
    void evaluatesDifferentPairsIndependentlyAndKeepsCreationOrder() {
        Fixture fixture = new Fixture();
        fixture.rates.put("USD/CAD", new BigDecimal("1.40"));
        fixture.rates.put("GBP/USD", new BigDecimal("1.20"));
        AlertView first = fixture.service.create(
                "USD/CAD", new BigDecimal("1.30"), Direction.ABOVE);
        fixture.clock.advance(Duration.ofSeconds(1));
        AlertView second = fixture.service.create(
                "GBP/USD", new BigDecimal("1.30"), Direction.ABOVE);

        List<AlertView> alerts = fixture.service.list();

        assertThat(alerts).extracting(AlertView::id).containsExactly(first.id(), second.id());
        assertThat(alerts).extracting(AlertView::triggered).containsExactly(true, false);
    }

    @Test
    void outageDoesNotFailCreateOrListAndPreservesAlreadyTriggeredAlert() {
        Fixture fixture = new Fixture();
        fixture.rates.put("USD/CAD", new BigDecimal("1.40"));
        AlertView fired = fixture.service.create(
                "USD/CAD", new BigDecimal("1.30"), Direction.ABOVE);
        fixture.xeDown = true;
        fixture.clock.advance(Duration.ofSeconds(1));

        AlertView createdDuringOutage = fixture.service.create(
                "GBP/USD", new BigDecimal("9.99"), Direction.ABOVE);
        List<AlertView> alertsDuringOutage = fixture.service.list();

        assertThat(createdDuringOutage.triggered()).isFalse();
        assertThat(createdDuringOutage.currentRate()).isNull();
        assertThat(alertsDuringOutage).hasSize(2);
        assertThat(alertsDuringOutage).allSatisfy(alert -> assertThat(alert.currentRate()).isNull());
        assertThat(alertsDuringOutage.getFirst().triggered()).isTrue();
        assertThat(alertsDuringOutage.getFirst().triggeredAt()).isEqualTo(fired.triggeredAt());
    }

    @Test
    void deleteReturnsFalseAfterFirstDeleteAndForUnknownId() {
        Fixture fixture = new Fixture();
        AlertView created = fixture.service.create(
                "USD/CAD", new BigDecimal("2.00"), Direction.ABOVE);

        assertThat(fixture.service.delete(created.id())).isTrue();
        assertThat(fixture.service.delete(created.id())).isFalse();
        assertThat(fixture.service.delete(UUID.randomUUID())).isFalse();
    }

    private static class Fixture {
        private final Map<String, BigDecimal> rates = new HashMap<>(Map.of(
                "USD/CAD", new BigDecimal("1.40"),
                "GBP/USD", new BigDecimal("1.25"),
                "EUR/USD", new BigDecimal("1.10")));
        private final MutableClock clock = new MutableClock(START);
        private final InMemoryAlertRepository repository = new InMemoryAlertRepository();
        private boolean xeDown;
        private final RateProvider provider = pair -> {
            if (xeDown) {
                throw new RateUnavailableException("XE unavailable");
            }
            return new Rate(pair, rates.get(pair), clock.instant().toString());
        };
        private final RateService rateService = new RateService(provider, clock, 0);
        private final AlertService service = new AlertService(
                repository, rateService, new AlertEvaluator(), clock);
    }
}
