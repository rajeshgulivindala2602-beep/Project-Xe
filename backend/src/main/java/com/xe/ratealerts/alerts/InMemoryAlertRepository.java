package com.xe.ratealerts.alerts;

import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAlertRepository implements AlertRepository {

    private final ConcurrentHashMap<UUID, Alert> alerts = new ConcurrentHashMap<>();

    @Override
    public Alert save(Alert alert) {
        alerts.put(alert.id(), alert);
        return alert;
    }

    @Override
    public boolean update(Alert alert) {
        return alerts.replace(alert.id(), alert) != null;
    }

    @Override
    public List<Alert> findAll() {
        return alerts.values().stream()
                .sorted(Comparator.comparing(Alert::createdAt).thenComparing(Alert::id))
                .toList();
    }

    @Override
    public boolean deleteById(UUID id) {
        return alerts.remove(id) != null;
    }
}
