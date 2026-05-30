package dev.reddragon.app.controllers;

import java.time.Instant;
import java.util.List;

import dev.reddragon.persistence.domains.AnalyticsSnapshotEntity;
import dev.reddragon.persistence.services.repositories.AnalyticsSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegimeHistoryControllerTest {

    @Test
    void historyCanBeScopedBySymbol() throws Exception {
        AnalyticsSnapshotRepository repository = mock(AnalyticsSnapshotRepository.class);
        when(repository.findBySymbolAndObservedAtAfterOrderByObservedAtDesc(eq("ASTS"), any()))
                .thenReturn(List.of(snapshot("ASTS", "SUPPORTIVE_TREND")));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new RegimeHistoryController(repository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/regime/history?symbol=asts&lookbackHours=48"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("ASTS"))
                .andExpect(jsonPath("$[0].regimeLabel").value("SUPPORTIVE_TREND"));

        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);
        verify(repository).findBySymbolAndObservedAtAfterOrderByObservedAtDesc(eq("ASTS"), since.capture());
        assertThat(since.getValue()).isBefore(Instant.now());
    }

    @Test
    void historyCanBeScopedBySymbolAndRegime() throws Exception {
        AnalyticsSnapshotRepository repository = mock(AnalyticsSnapshotRepository.class);
        when(repository.findBySymbolAndRegimeLabelAndObservedAtAfterOrderByObservedAtDesc(
                eq("ASTS"),
                eq("SUPPORTIVE_TREND"),
                any()))
                .thenReturn(List.of(snapshot("ASTS", "SUPPORTIVE_TREND")));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new RegimeHistoryController(repository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/regime/history?symbol=asts&regime=SUPPORTIVE_TREND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("ASTS"));

        verify(repository).findBySymbolAndRegimeLabelAndObservedAtAfterOrderByObservedAtDesc(
                eq("ASTS"),
                eq("SUPPORTIVE_TREND"),
                any());
    }

    private AnalyticsSnapshotEntity snapshot(String symbol, String regimeLabel) {
        return AnalyticsSnapshotEntity.builder()
                .candidateId("cand-1")
                .symbol(symbol)
                .observedAt(Instant.parse("2026-05-28T12:00:00Z"))
                .regimeLabel(regimeLabel)
                .regimeCompatibilityScore(0.75)
                .asymmetryScore(0.80)
                .equilibriumQualityScore(0.70)
                .reflexivityPotentialScore(0.65)
                .deploymentConfidenceScore(0.72)
                .reasonNotes("ok")
                .build();
    }
}
