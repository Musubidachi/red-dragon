package dev.reddragon.app.services.pipeline;

import java.time.Instant;
import java.util.List;

import dev.reddragon.analytics.services.DeterministicAnalyticsService;
import dev.reddragon.analytics.services.marketscoring.MarketDataSnapshotScorer;
import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.marketdata.services.MarketFeatureCalculator;
import dev.reddragon.persistence.services.PersistenceMapper;
import dev.reddragon.persistence.services.repositories.AnalyticsSnapshotRepository;
import dev.reddragon.persistence.services.repositories.CandidateRepository;
import dev.reddragon.persistence.services.repositories.MarketBarRepository;
import dev.reddragon.persistence.services.repositories.MarketSnapshotRepository;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import dev.reddragon.validation.config.ValidationProfile;
import dev.reddragon.validation.services.engine.CandidateValidationInputFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CandidatePipelineOrchestratorTest {

    @Test
    void duplicateCandidateSkipsPipelineWorkAndRecordsDuplicateMetric() {
        CandidateRepository candidateRepository = mock(CandidateRepository.class);
        MarketFeatureCalculator marketFeatureCalculator = mock(MarketFeatureCalculator.class);
        MarketDataSnapshotScorer marketDataSnapshotScorer = mock(MarketDataSnapshotScorer.class);
        DeterministicAnalyticsService analyticsService = mock(DeterministicAnalyticsService.class);
        CandidateValidationInputFactory validationInputFactory = mock(CandidateValidationInputFactory.class);
        ValidationServiceFactory validationServiceFactory = mock(ValidationServiceFactory.class);
        ValidationVerdictRepository validationVerdictRepository = mock(ValidationVerdictRepository.class);
        MarketBarRepository marketBarRepository = mock(MarketBarRepository.class);
        MarketSnapshotRepository marketSnapshotRepository = mock(MarketSnapshotRepository.class);
        AnalyticsSnapshotRepository analyticsSnapshotRepository = mock(AnalyticsSnapshotRepository.class);
        PersistenceMapper persistenceMapper = mock(PersistenceMapper.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        CandidatePipelineOrchestrator orchestrator = new CandidatePipelineOrchestrator(
                marketFeatureCalculator,
                marketDataSnapshotScorer,
                analyticsService,
                validationInputFactory,
                validationServiceFactory,
                candidateRepository,
                validationVerdictRepository,
                marketBarRepository,
                marketSnapshotRepository,
                analyticsSnapshotRepository,
                persistenceMapper,
                meterRegistry);
        TradeCandidate candidate = candidate();
        when(candidateRepository.existsById("cand-1")).thenReturn(true);

        var result = orchestrator.process(candidate, List.of(), ValidationProfile.STANDARD);

        assertThat(result.duplicate()).isTrue();
        assertThat(result.candidate()).isEqualTo(candidate);
        assertThat(meterRegistry.counter("reddragon.pipeline.candidate.duplicate").count()).isEqualTo(1.0);
        verify(candidateRepository).existsById("cand-1");
        verify(candidateRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(
                marketFeatureCalculator,
                marketDataSnapshotScorer,
                analyticsService,
                validationInputFactory,
                validationServiceFactory,
                validationVerdictRepository,
                marketBarRepository,
                marketSnapshotRepository,
                analyticsSnapshotRepository,
                persistenceMapper);
    }

    private TradeCandidate candidate() {
        return TradeCandidate.builder()
                .candidateId("cand-1")
                .symbol("ASTS")
                .companyName("AST SpaceMobile")
                .catalystType(CandidateCatalystType.CONTRACT)
                .sourceType(SourceType.MANUAL)
                .observedAt(Instant.parse("2026-05-27T12:00:00Z"))
                .headline("Headline")
                .summary("Summary")
                .structuralRealityScore(0.80)
                .materialSignificanceScore(0.70)
                .earlynessScore(0.60)
                .reflexivityPotentialScore(0.75)
                .build();
    }
}
