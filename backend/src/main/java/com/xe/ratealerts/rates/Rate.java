package com.xe.ratealerts.rates;

import java.math.BigDecimal;

public record Rate(String pair, BigDecimal rate, String asOf) {
}
