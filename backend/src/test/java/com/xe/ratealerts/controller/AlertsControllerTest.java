package com.xe.ratealerts.controller;

import com.xe.ratealerts.alerts.AlertService;
import com.xe.ratealerts.alerts.AlertView;
import com.xe.ratealerts.alerts.Direction;
import com.xe.ratealerts.alerts.InvalidAlertException;
import com.xe.ratealerts.rates.RateUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertsController.class)
class AlertsControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2025-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;

    @Test
    void validCreateReturnsCreatedLocationAndAlertView() throws Exception {
        UUID id = UUID.randomUUID();
        when(alertService.create("USD/CAD", new BigDecimal("1.30"), Direction.ABOVE))
                .thenReturn(view(id, true, CREATED_AT));

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pair":"USD/CAD","threshold":1.30,"direction":"above"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/alerts/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.pair").value("USD/CAD"))
                .andExpect(jsonPath("$.direction").value("above"))
                .andExpect(jsonPath("$.triggered").value(true));
        verify(alertService).create("USD/CAD", new BigDecimal("1.30"), Direction.ABOVE);
    }

    @Test
    void invalidAlertExceptionReturnsBadRequestMessage() throws Exception {
        when(alertService.create(eq("XXX/YYY"), any(), eq(Direction.ABOVE)))
                .thenThrow(new InvalidAlertException("Unknown pair 'XXX/YYY'."));

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pair":"XXX/YYY","threshold":1.30,"direction":"above"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown pair 'XXX/YYY'."));
    }

    @Test
    void invalidDirectionReturnsBadRequestBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pair":"USD/CAD","threshold":1.30,"direction":"sideways"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(alertService);
    }

    @Test
    void nonNumericThresholdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pair":"USD/CAD","threshold":"high","direction":"above"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(alertService);
    }

    @Test
    void getReturnsAlertViewWithTriggeredAtAsIsoString() throws Exception {
        Instant triggeredAt = Instant.parse("2025-01-02T03:04:05Z");
        UUID id = UUID.randomUUID();
        when(alertService.list()).thenReturn(List.of(view(id, true, triggeredAt)));

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].triggered").value(true))
                .andExpect(jsonPath("$[0].triggeredAt").value("2025-01-02T03:04:05Z"));
    }

    @Test
    void rateUnavailableReturnsFriendlyBadGatewayResponse() throws Exception {
        when(alertService.list()).thenThrow(new RateUnavailableException("XE unavailable"));

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value(
                        "Exchange rates are temporarily unavailable. Please try again."));
    }

    @Test
    void deleteReturnsNoContentWhenDeletedAndNotFoundWhenUnknown() throws Exception {
        UUID id = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();
        when(alertService.delete(id)).thenReturn(true);
        when(alertService.delete(unknownId)).thenReturn(false);

        mockMvc.perform(delete("/api/alerts/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/alerts/{id}", unknownId))
                .andExpect(status().isNotFound());
    }

    private AlertView view(UUID id, boolean triggered, Instant triggeredAt) {
        return new AlertView(
                id,
                "USD/CAD",
                new BigDecimal("1.30"),
                Direction.ABOVE,
                triggered,
                CREATED_AT,
                triggered ? triggeredAt : null,
                new BigDecimal("1.40"));
    }
}
