package com.xe.ratealerts.rates;

public interface RateProvider {
    Rate getRate(String pair);
}
