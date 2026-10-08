package com.xe.ratealerts.controller;

import com.xe.ratealerts.alerts.AlertService;
import com.xe.ratealerts.alerts.AlertView;
import com.xe.ratealerts.alerts.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
public class AlertsController {

    private final AlertService alertService;

    public AlertsController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertView> list() {
        return alertService.list();
    }

    @PostMapping
    public ResponseEntity<AlertView> create(@RequestBody CreateAlertRequest request) {
        AlertView alert = alertService.create(request.pair(), request.threshold(), request.direction());
        return ResponseEntity.created(URI.create("/api/alerts/" + alert.id())).body(alert);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return alertService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    public record CreateAlertRequest(String pair, BigDecimal threshold, Direction direction) {
    }
}
