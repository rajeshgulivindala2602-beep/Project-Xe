package com.xe.ratealerts.rates;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateServiceTest {

    @Test
    void returnsRatesInSupportedPairOrder() {
        List<String> requestedPairs = new ArrayList<>();
        RateService service = new RateService(pair -> {
            requestedPairs.add(pair);
            return new Rate(pair, BigDecimal.ONE, "now");
        });

        List<Rate> rates = service.getRates();

        assertThat(requestedPairs).containsExactly("USD/CAD", "GBP/USD", "EUR/USD");
        assertThat(rates).extracting(Rate::pair).containsExactly("USD/CAD", "GBP/USD", "EUR/USD");
    }

    @Test
    void propagatesProviderException() {
        RuntimeException failure = new RuntimeException("provider failed");
        RateService service = new RateService(pair -> {
            throw failure;
        });

        assertThatThrownBy(service::getRates).isSameAs(failure);
    }
}
