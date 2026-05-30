package dev.reddragon.app.controllers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import dev.reddragon.persistence.domains.CandidateEntity;
import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import dev.reddragon.persistence.domains.ValidationVerdictReasonEntity;
import dev.reddragon.persistence.services.repositories.AnalyticsSnapshotRepository;
import dev.reddragon.persistence.services.repositories.CandidateRepository;
import dev.reddragon.persistence.services.repositories.MarketSnapshotRepository;
import dev.reddragon.persistence.services.repositories.TraderNoteRepository;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import dev.reddragon.persistence.services.repositories.VerdictOverrideRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CandidateHistoryControllerTest {

    @Test
    void symbolHistoryNormalizesSymbolAndCapsLimit() throws Exception {
        CandidateRepository candidateRepository = mock(CandidateRepository.class);
        ValidationVerdictRepository verdictRepository = mock(ValidationVerdictRepository.class);
        TraderNoteRepository traderNoteRepository = mock(TraderNoteRepository.class);
        VerdictOverrideRepository overrideRepository = mock(VerdictOverrideRepository.class);
        CandidateHistoryController controller = controller(
                candidateRepository,
                verdictRepository,
                mock(MarketSnapshotRepository.class),
                mock(AnalyticsSnapshotRepository.class),
                traderNoteRepository,
                overrideRepository);
        ValidationVerdictEntity verdict = verdict("cand-1", "ASTS");
        when(verdictRepository.findBySymbolOrderByCreatedAtDesc(eq("ASTS"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(verdict));
        when(traderNoteRepository.countByCandidateId("cand-1")).thenReturn(2L);
        when(overrideRepository.findTopByCandidateIdOrderByOverriddenAtDesc("cand-1"))
                .thenReturn(Optional.empty());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/candidates/asts/history?limit=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].candidateId").value("cand-1"))
                .andExpect(jsonPath("$[0].noteCount").value(2))
                .andExpect(jsonPath("$[0].reasonCodes[0]").value("ASYMMETRY_CONFIRMED"))
                .andExpect(jsonPath("$[0].explanations[0]").value("Asymmetry confirmed."));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(verdictRepository).findBySymbolOrderByCreatedAtDesc(eq("ASTS"), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void candidateDetailReturnsCandidateContextAndRelatedCollections() throws Exception {
        CandidateRepository candidateRepository = mock(CandidateRepository.class);
        ValidationVerdictRepository verdictRepository = mock(ValidationVerdictRepository.class);
        MarketSnapshotRepository marketSnapshotRepository = mock(MarketSnapshotRepository.class);
        AnalyticsSnapshotRepository analyticsSnapshotRepository = mock(AnalyticsSnapshotRepository.class);
        TraderNoteRepository traderNoteRepository = mock(TraderNoteRepository.class);
        VerdictOverrideRepository overrideRepository = mock(VerdictOverrideRepository.class);
        CandidateEntity candidate = CandidateEntity.builder()
                .candidateId("cand-1")
                .symbol("ASTS")
                .companyName("AST SpaceMobile")
                .catalystType("CONTRACT")
                .sourceType("SEC_EDGAR")
                .observedAt(Instant.parse("2026-05-27T12:00:00Z"))
                .headline("Headline")
                .summary("Summary")
                .build();
        when(candidateRepository.findById("cand-1")).thenReturn(Optional.of(candidate));
        when(verdictRepository.findByCandidateIdOrderByCreatedAtDesc(eq("cand-1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(verdict("cand-1", "ASTS")));
        when(marketSnapshotRepository.findTop25ByCandidateIdOrderByObservedAtDesc("cand-1")).thenReturn(List.of());
        when(analyticsSnapshotRepository.findTop25ByCandidateIdOrderByObservedAtDesc("cand-1")).thenReturn(List.of());
        when(traderNoteRepository.findByCandidateIdOrderByCreatedAtDesc("cand-1")).thenReturn(List.of());
        when(overrideRepository.findByCandidateIdOrderByOverriddenAtDesc("cand-1")).thenReturn(List.of());
        when(traderNoteRepository.countByCandidateId("cand-1")).thenReturn(0L);
        when(overrideRepository.findTopByCandidateIdOrderByOverriddenAtDesc("cand-1"))
                .thenReturn(Optional.empty());
        CandidateHistoryController controller = controller(
                candidateRepository,
                verdictRepository,
                marketSnapshotRepository,
                analyticsSnapshotRepository,
                traderNoteRepository,
                overrideRepository);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/candidates/id/cand-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateId").value("cand-1"))
                .andExpect(jsonPath("$.companyName").value("AST SpaceMobile"))
                .andExpect(jsonPath("$.verdicts[0].symbol").value("ASTS"))
                .andExpect(jsonPath("$.marketSnapshots").isArray())
                .andExpect(jsonPath("$.analyticsSnapshots").isArray())
                .andExpect(jsonPath("$.notes").isArray())
                .andExpect(jsonPath("$.overrides").isArray());
    }

    private CandidateHistoryController controller(
            CandidateRepository candidateRepository,
            ValidationVerdictRepository verdictRepository,
            MarketSnapshotRepository marketSnapshotRepository,
            AnalyticsSnapshotRepository analyticsSnapshotRepository,
            TraderNoteRepository traderNoteRepository,
            VerdictOverrideRepository overrideRepository
    ) {
        return new CandidateHistoryController(
                candidateRepository,
                verdictRepository,
                marketSnapshotRepository,
                analyticsSnapshotRepository,
                traderNoteRepository,
                overrideRepository);
    }

    private ValidationVerdictEntity verdict(String candidateId, String symbol) {
        return ValidationVerdictEntity.builder()
                .candidateId(candidateId)
                .symbol(symbol)
                .verdict("PASS")
                .deploymentTier("STANDARD")
                .score(0.82)
                .createdAt(Instant.parse("2026-05-27T12:05:00Z"))
                .reasons(List.of(ValidationVerdictReasonEntity.builder()
                        .reasonCode("ASYMMETRY_CONFIRMED")
                        .explanation("Asymmetry confirmed.")
                        .sortOrder((short) 0)
                        .build()))
                .build();
    }
}
