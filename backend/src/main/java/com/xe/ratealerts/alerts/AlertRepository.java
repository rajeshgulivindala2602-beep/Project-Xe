package com.xe.ratealerts.alerts;

import java.util.List;
import java.util.UUID;

public interface AlertRepository {
    Alert save(Alert alert);

    /**
     * Replaces an existing alert only, never creates one, so evaluating an alert
     * that was deleted meanwhile cannot resurrect it; returns false if its id no longer exists.
     */
    boolean update(Alert alert);

    List<Alert> findAll();

    boolean deleteById(UUID id);
}
