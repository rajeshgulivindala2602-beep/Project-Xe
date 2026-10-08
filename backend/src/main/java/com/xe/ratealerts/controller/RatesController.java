package com.xe.ratealerts.controller;

import com.xe.ratealerts.rates.Rate;
import com.xe.ratealerts.rates.RateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
public class RatesController {

    private final RateService rateService;

    public RatesController(RateService rateService) {
        this.rateService = rateService;
    }

    @GetMapping
    public List<Rate> getRates() {
        return rateService.getRates();
    }
}
