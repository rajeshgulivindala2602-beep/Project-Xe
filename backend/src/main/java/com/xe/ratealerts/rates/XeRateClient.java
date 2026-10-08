package com.xe.ratealerts.rates;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class XeRateClient implements RateProvider {

    private final RestTemplate xeRestTemplate;
    private final String baseUrl;

    public XeRateClient(
            RestTemplate xeRestTemplate,
            @Value("${xecd.base-url:https://xecdapi.xe.com}") String baseUrl) {
        this.xeRestTemplate = xeRestTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public Rate getRate(String pair) {
        String[] currencies = pair.split("/", -1);
        if (currencies.length != 2) {
            throw new IllegalArgumentException("Pair must contain exactly two currencies separated by '/'.");
        }

        JsonNode response;
        try {
            response = xeRestTemplate.getForObject(
                    baseUrl + "/v1/convert_from.json/?from={from}&to={to}",
                    JsonNode.class,
                    currencies[0],
                    currencies[1]);
        } catch (RestClientException exception) {
            throw new RateUnavailableException("Unable to retrieve rate for " + pair + ".", exception);
        }

        JsonNode mid = response == null ? null : response.path("to").path(0).path("mid");
        if (mid == null || !mid.isNumber()) {
            throw new RateUnavailableException("XE returned an invalid rate response for " + pair + ".");
        }

        String timestamp = response.path("timestamp").asText();
        BigDecimal rate = mid.decimalValue().setScale(4, RoundingMode.HALF_UP);
        return new Rate(pair, rate, timestamp);
    }
}
