package com.xe.ratealerts.alerts;

import java.util.List;
import java.util.UUID;

public interface AlertRepository {
    Alert save(Alert alert);

    List<Alert> findAll();

    boolean deleteById(UUID id);
}
