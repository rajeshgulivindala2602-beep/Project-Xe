package com.xe.ratealerts.rates;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateService {

    public static final List<String> SUPPORTED_PAIRS = List.of("USD/CAD", "GBP/USD", "EUR/USD");

    private final RateProvider rateProvider;
    private final Clock clock;
    private final Duration cacheTtl;
    private final ConcurrentHashMap<String, CachedRate> cache = new ConcurrentHashMap<>();

    public RateService(
            RateProvider rateProvider,
            Clock clock,
            @Value("${rates.cache-ttl-seconds:30}") long cacheTtlSeconds) {
        this.rateProvider = rateProvider;
        this.clock = clock;
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
    }

    public Rate getRate(String pair) {
        Instant now = clock.instant();
        CachedRate cached = cache.get(pair);
        if (cacheTtl.isPositive() && cached != null
                && Duration.between(cached.fetchedAt(), now).compareTo(cacheTtl) < 0) {
            return cached.rate();
        }

        Rate rate = rateProvider.getRate(pair);
        if (!cacheTtl.isZero() && !cacheTtl.isNegative()) {
            // Concurrent cold-cache requests may both fetch; this avoids locking and is harmless.
            cache.put(pair, new CachedRate(rate, clock.instant()));
        }
        return rate;
    }

    public List<Rate> getRates() {
        return SUPPORTED_PAIRS.stream()
                .map(this::getRate)
                .toList();
    }

    private record CachedRate(Rate rate, Instant fetchedAt) {
    }
}
