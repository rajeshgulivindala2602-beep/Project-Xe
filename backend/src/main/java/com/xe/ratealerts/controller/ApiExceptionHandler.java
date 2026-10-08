package com.xe.ratealerts.controller;

import com.xe.ratealerts.alerts.InvalidAlertException;
import com.xe.ratealerts.rates.RateUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String INVALID_BODY_MESSAGE =
            "Invalid request body. Expected pair, a numeric threshold, and direction 'above' or 'below'.";

    @ExceptionHandler(InvalidAlertException.class)
    public ResponseEntity<Map<String, String>> handleInvalidAlert(InvalidAlertException exception) {
        return ResponseEntity.badRequest().body(error(exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(error(INVALID_BODY_MESSAGE));
    }

    @ExceptionHandler(RateUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleUnavailableRate(RateUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                error("Exchange rates are temporarily unavailable. Please try again."));
    }

    private Map<String, String> error(String message) {
        return Map.of("error", message);
    }
}
