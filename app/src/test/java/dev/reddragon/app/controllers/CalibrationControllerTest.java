package dev.reddragon.app.controllers;

import dev.reddragon.app.services.pipeline.CalibrationOutcomeService;
import dev.reddragon.persistence.domains.CalibrationOutcomeEntity;
import dev.reddragon.persistence.services.repositories.CalibrationOutcomeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CalibrationControllerTest {

    @Test
    void countEndpointReturnsDedicatedScalarShape() throws Exception {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.count()).thenReturn(12L);
        CalibrationOutcomeService service = new CalibrationOutcomeService(null, repository);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CalibrationController(service)).build();

        mockMvc.perform(get("/api/calibration/outcomes/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric").value("outcomeCount"))
                .andExpect(jsonPath("$.count").value(12))
                .andExpect(jsonPath("$.sampleSize").doesNotExist())
                .andExpect(jsonPath("$.winRate").doesNotExist());
    }

    @Test
    void summaryDetailsReturnsConsolidatedMetrics() throws Exception {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                outcome("c1", "ASTS", 0.20, 0.10, 8, true),
                outcome("c2", "NVDA", -0.04, 0.18, 4, false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(null, repository);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CalibrationController(service)).build();

        mockMvc.perform(get("/api/calibration/summary/details?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleSize").value(2))
                .andExpect(jsonPath("$.winRate").value(0.5))
                .andExpect(jsonPath("$.minReturn").value(-0.04))
                .andExpect(jsonPath("$.maxReturn").value(0.20))
                .andExpect(jsonPath("$.averageHoldingDays").value(6.0));
    }

    @Test
    void scalarSummaryEndpointsAdvertiseDetailsReplacement() throws Exception {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                outcome("c1", "ASTS", 0.20, 0.10, 8, true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(null, repository);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CalibrationController(service)).build();

        mockMvc.perform(get("/api/calibration/summary/win-rate"))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string(
                        "X-Red-Dragon-Deprecated-Endpoint",
                        "Use /api/calibration/summary/details instead."))
                .andExpect(jsonPath("$.metric").value("winRate"))
                .andExpect(jsonPath("$.value").value(1.0));
    }

    @Test
    void summaryBatchReturnsPerSymbolSummaries() throws Exception {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("ASTS")).thenReturn(List.of(
                outcome("c1", "ASTS", 0.20, 0.10, 8, true)
        ));
        when(repository.findTop100BySymbolOrderByObservedAtDesc("NVDA")).thenReturn(List.of(
                outcome("c2", "NVDA", -0.04, 0.18, 4, false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(null, repository);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CalibrationController(service)).build();

        mockMvc.perform(get("/api/calibration/summary/batch?symbols=asts,nvda,asts&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ASTS.sampleSize").value(1))
                .andExpect(jsonPath("$.ASTS.winRate").value(1.0))
                .andExpect(jsonPath("$.NVDA.sampleSize").value(1))
                .andExpect(jsonPath("$.NVDA.winRate").value(0.0));
    }

    private CalibrationOutcomeEntity outcome(
            String candidateId,
            String symbol,
            double realizedReturn,
            double maxDrawdown,
            int daysHeld,
            boolean thesisWorked
    ) {
        return CalibrationOutcomeEntity.builder()
                .candidateId(candidateId)
                .symbol(symbol)
                .observedAt(Instant.parse("2026-05-28T12:00:00Z"))
                .structuralRealityScore(0.8)
                .materialSignificanceScore(0.7)
                .earlynessScore(0.6)
                .equilibriumQualityScore(0.7)
                .reflexivityPotentialScore(0.6)
                .asymmetryScore(0.7)
                .regimeCompatibilityScore(0.6)
                .deploymentConfidenceScore(0.8)
                .realizedReturn(realizedReturn)
                .maxDrawdown(maxDrawdown)
                .daysHeld(daysHeld)
                .thesisWorked(thesisWorked)
                .build();
    }
}
