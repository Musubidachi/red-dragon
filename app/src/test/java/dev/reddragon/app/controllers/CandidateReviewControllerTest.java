package dev.reddragon.app.controllers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import dev.reddragon.app.services.review.CandidateReviewService;
import dev.reddragon.app.services.review.CandidateReviewStreamService;
import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import dev.reddragon.persistence.domains.VerdictOverrideEntity;
import dev.reddragon.persistence.services.repositories.AnalyticsSnapshotRepository;
import dev.reddragon.persistence.services.repositories.TraderNoteRepository;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import dev.reddragon.persistence.services.repositories.VerdictOverrideRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CandidateReviewControllerTest {

    @Test
    void listCandidatesIncludesVerdictIdNoteCountAndOverrideBadgeData() throws Exception {
        ValidationVerdictRepository verdictRepository = mock(ValidationVerdictRepository.class);
        AnalyticsSnapshotRepository analyticsSnapshotRepository = mock(AnalyticsSnapshotRepository.class);
        TraderNoteRepository traderNoteRepository = mock(TraderNoteRepository.class);
        VerdictOverrideRepository overrideRepository = mock(VerdictOverrideRepository.class);
        ValidationVerdictEntity verdict = ValidationVerdictEntity.builder()
                .candidateId("cand-1")
                .symbol("ASTS")
                .verdict("WATCH")
                .deploymentTier("STANDARD")
                .score(0.64)
                .createdAt(Instant.parse("2026-05-28T12:00:00Z"))
                .build();
        ReflectionTestUtils.setField(verdict, "id", 42L);
        VerdictOverrideEntity override = VerdictOverrideEntity.builder()
                .verdictId(42L)
                .candidateId("cand-1")
                .symbol("ASTS")
                .originalVerdict("WATCH")
                .overrideVerdict("PASS")
                .reason("Trader override")
                .overriddenAt(Instant.parse("2026-05-28T12:05:00Z"))
                .build();
        when(verdictRepository.findLatestPerCandidateSince(any(), any()))
                .thenReturn(List.of(verdict));
        when(analyticsSnapshotRepository.findTopByCandidateIdOrderByObservedAtDesc("cand-1"))
                .thenReturn(Optional.empty());
        when(traderNoteRepository.countByCandidateId("cand-1")).thenReturn(3L);
        when(overrideRepository.findTopByCandidateIdOrderByOverriddenAtDesc("cand-1"))
                .thenReturn(Optional.of(override));
        CandidateReviewService reviewService = new CandidateReviewService(
                verdictRepository,
                analyticsSnapshotRepository,
                traderNoteRepository,
                overrideRepository);
        CandidateReviewStreamService streamService = new CandidateReviewStreamService(reviewService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CandidateReviewController(
                reviewService,
                streamService)).build();

        mockMvc.perform(get("/api/review/candidates?verdicts=WATCH&lookbackHours=24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].verdictId").value(42))
                .andExpect(jsonPath("$[0].candidateId").value("cand-1"))
                .andExpect(jsonPath("$[0].noteCount").value(3))
                .andExpect(jsonPath("$[0].overrideVerdict").value("PASS"));
    }

    @Test
    void streamCandidatesStartsSseResponse() throws Exception {
        CandidateReviewService reviewService = mock(CandidateReviewService.class);
        when(reviewService.listCandidates("WATCH", 12)).thenReturn(List.of());
        CandidateReviewStreamService streamService = new CandidateReviewStreamService(reviewService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CandidateReviewController(
                reviewService,
                streamService)).build();

        mockMvc.perform(get("/api/review/candidates/stream?verdicts=WATCH&lookbackHours=12"))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
    }
}
