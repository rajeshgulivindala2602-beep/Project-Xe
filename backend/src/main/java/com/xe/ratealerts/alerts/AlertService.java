package com.xe.ratealerts.alerts;

import com.xe.ratealerts.rates.Rate;
import com.xe.ratealerts.rates.RateService;
import com.xe.ratealerts.rates.RateUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Alerts are evaluated lazily when listed or created (no background job yet),
 * so an alert only changes state when someone looks.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final RateService rateService;
    private final AlertEvaluator alertEvaluator;
    private final Clock clock;

    public AlertService(
            AlertRepository alertRepository,
            RateService rateService,
            AlertEvaluator alertEvaluator,
            Clock clock) {
        this.alertRepository = alertRepository;
        this.rateService = rateService;
        this.alertEvaluator = alertEvaluator;
        this.clock = clock;
    }

    public AlertView create(String pair, BigDecimal threshold, Direction direction) {
        if (pair == null || !RateService.SUPPORTED_PAIRS.contains(pair)) {
            throw new InvalidAlertException(
                    "Unknown pair '" + pair + "'. Supported pairs: "
                            + String.join(", ", RateService.SUPPORTED_PAIRS) + ".");
        }
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAlertException("Threshold must be a number greater than 0.");
        }
        if (direction == null) {
            throw new InvalidAlertException("Direction must be 'above' or 'below'.");
        }

        Alert alert = alertRepository.save(Alert.create(pair, threshold, direction, clock.instant()));
        return evaluateAll(List.of(alert)).getFirst();
    }

    public List<AlertView> list() {
        return evaluateAll(alertRepository.findAll());
    }

    public boolean delete(UUID id) {
        return alertRepository.deleteById(id);
    }

    private List<AlertView> evaluateAll(List<Alert> alerts) {
        Instant now = clock.instant();
        Map<String, Optional<BigDecimal>> ratesByPair = new HashMap<>();

        return alerts.stream()
                .map(alert -> {
                    Optional<BigDecimal> rate = ratesByPair.computeIfAbsent(
                            alert.pair(), this::fetchRate);
                    Alert evaluated = alert;
                    if (rate.isPresent()) {
                        evaluated = alertEvaluator.evaluate(alert, rate.get(), now);
                        if (!evaluated.equals(alert)) {
                            alertRepository.update(evaluated);
                        }
                    }
                    return AlertView.of(evaluated, rate.orElse(null));
                })
                .toList();
    }

    private Optional<BigDecimal> fetchRate(String pair) {
        try {
            Rate rate = rateService.getRate(pair);
            return Optional.of(rate.rate());
        } catch (RateUnavailableException exception) {
            log.warn("Could not fetch current rate for alert pair {}.", pair, exception);
            return Optional.empty();
        }
    }
}
