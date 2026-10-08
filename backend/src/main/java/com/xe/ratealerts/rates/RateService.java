package com.xe.ratealerts.rates;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RateService {

    public static final List<String> SUPPORTED_PAIRS = List.of("USD/CAD", "GBP/USD", "EUR/USD");

    private final RateProvider rateProvider;

    public RateService(RateProvider rateProvider) {
        this.rateProvider = rateProvider;
    }

    public List<Rate> getRates() {
        return SUPPORTED_PAIRS.stream()
                .map(rateProvider::getRate)
                .toList();
    }
}
