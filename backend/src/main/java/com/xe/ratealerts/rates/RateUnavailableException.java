package com.xe.ratealerts.rates;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class RateUnavailableException extends RuntimeException {

    public RateUnavailableException(String message) {
        super(message);
    }

    public RateUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
