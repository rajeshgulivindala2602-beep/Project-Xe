package com.xe.ratealerts.rates;

import com.xe.ratealerts.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateServiceTest {

    private static final Instant START = Instant.parse("2025-01-01T00:00:00Z");

    @Test
    void returnsRatesInSupportedPairOrder() {
        List<String> requestedPairs = new ArrayList<>();
        RateService service = service(pair -> {
            requestedPairs.add(pair);
            return rate(pair);
        }, new MutableClock(START), 30);

        List<Rate> rates = service.getRates();

        assertThat(requestedPairs).containsExactly("USD/CAD", "GBP/USD", "EUR/USD");
        assertThat(rates).extracting(Rate::pair).containsExactly("USD/CAD", "GBP/USD", "EUR/USD");
    }

    @Test
    void reusesRateWithinTtl() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(START);
        RateService service = service(pair -> {
            calls.incrementAndGet();
            return rate(pair);
        }, clock, 30);

        Rate first = service.getRate("USD/CAD");
        clock.advance(Duration.ofSeconds(29));
        Rate second = service.getRate("USD/CAD");

        assertThat(second).isEqualTo(first);
        assertThat(calls).hasValue(1);
    }

    @Test
    void refetchesRateWhenTtlHasPassed() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(START);
        RateService service = service(pair -> {
            calls.incrementAndGet();
            return rate(pair);
        }, clock, 30);

        service.getRate("USD/CAD");
        clock.advance(Duration.ofSeconds(30));
        service.getRate("USD/CAD");

        assertThat(calls).hasValue(2);
    }

    @Test
    void cachesEachPairSeparately() {
        List<String> requestedPairs = new ArrayList<>();
        RateService service = service(pair -> {
            requestedPairs.add(pair);
            return rate(pair);
        }, new MutableClock(START), 30);

        service.getRate("USD/CAD");
        service.getRate("GBP/USD");
        service.getRate("USD/CAD");
        service.getRate("GBP/USD");

        assertThat(requestedPairs).containsExactly("USD/CAD", "GBP/USD");
    }

    @Test
    void ttlZeroDisablesCaching() {
        AtomicInteger calls = new AtomicInteger();
        RateService service = service(pair -> {
            calls.incrementAndGet();
            return rate(pair);
        }, new MutableClock(START), 0);

        service.getRate("USD/CAD");
        service.getRate("USD/CAD");

        assertThat(calls).hasValue(2);
    }

    @Test
    void propagatesProviderFailureWithoutCachingIt() {
        AtomicInteger calls = new AtomicInteger();
        RuntimeException failure = new RuntimeException("provider failed");
        RateService service = service(pair -> {
            if (calls.incrementAndGet() == 1) {
                throw failure;
            }
            return rate(pair);
        }, new MutableClock(START), 30);

        assertThatThrownBy(() -> service.getRate("USD/CAD")).isSameAs(failure);
        assertThat(service.getRate("USD/CAD")).isEqualTo(rate("USD/CAD"));
        assertThat(calls).hasValue(2);
    }

    private RateService service(RateProvider provider, MutableClock clock, long ttlSeconds) {
        return new RateService(provider, clock, ttlSeconds);
    }

    private Rate rate(String pair) {
        return new Rate(pair, BigDecimal.ONE, START.toString());
    }
}
