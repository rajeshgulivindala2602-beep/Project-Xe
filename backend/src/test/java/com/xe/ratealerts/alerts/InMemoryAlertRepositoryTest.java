package com.xe.ratealerts.alerts;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAlertRepositoryTest {

    private final InMemoryAlertRepository repository = new InMemoryAlertRepository();

    @Test
    void listsAlertsOldestFirst() {
        Alert newest = alert(UUID.fromString("00000000-0000-0000-0000-000000000002"), "2025-01-02T00:00:00Z");
        Alert oldest = alert(UUID.fromString("00000000-0000-0000-0000-000000000003"), "2025-01-01T00:00:00Z");
        repository.save(newest);
        repository.save(oldest);

        assertThat(repository.findAll()).containsExactly(oldest, newest);
    }

    @Test
    void savingSameIdReplacesExistingAlert() {
        UUID id = UUID.randomUUID();
        Alert original = alert(id, "2025-01-01T00:00:00Z");
        Alert replacement = new Alert(
                id, "GBP/USD", new BigDecimal("2.0"), Direction.BELOW,
                Instant.parse("2025-01-02T00:00:00Z"), null);
        repository.save(original);

        assertThat(repository.save(replacement)).isEqualTo(replacement);
        assertThat(repository.findAll()).containsExactly(replacement);
    }

    @Test
    void deleteReturnsWhetherAlertExisted() {
        UUID id = UUID.randomUUID();
        repository.save(alert(id, "2025-01-01T00:00:00Z"));

        assertThat(repository.deleteById(id)).isTrue();
        assertThat(repository.deleteById(id)).isFalse();
        assertThat(repository.deleteById(UUID.randomUUID())).isFalse();
    }

    private Alert alert(UUID id, String createdAt) {
        return new Alert(
                id, "USD/CAD", new BigDecimal("1.5"), Direction.ABOVE,
                Instant.parse(createdAt), null);
    }
}
