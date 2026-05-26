package dev.reddragon.app.services.pipeline;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.analytics.services.DeterministicAnalyticsService;
import dev.reddragon.analytics.services.marketscoring.MarketDataSnapshotScorer;
import dev.reddragon.app.models.PipelineRunResult;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.marketdata.services.MarketFeatureCalculator;
import dev.reddragon.persistence.services.PersistenceMapper;
import dev.reddragon.persistence.services.repositories.AnalyticsSnapshotRepository;
import dev.reddragon.persistence.services.repositories.CandidateRepository;
import dev.reddragon.persistence.services.repositories.MarketBarRepository;
import dev.reddragon.persistence.services.repositories.MarketSnapshotRepository;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import dev.reddragon.validation.config.ValidationProfile;
import dev.reddragon.domain.models.ValidationAudit;
import dev.reddragon.domain.models.ValidationResult;
import dev.reddragon.validation.services.ValidationService;
import dev.reddragon.validation.services.engine.CandidateValidationInputFactory;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CandidatePipelineOrchestrator {

    private final MarketFeatureCalculator marketFeatureCalculator;
    private final MarketDataSnapshotScorer marketDataSnapshotScorer;
    private final DeterministicAnalyticsService analyticsService;
    private final CandidateValidationInputFactory validationInputFactory;
    private final ValidationServiceFactory validationServiceFactory;
    private final CandidateRepository candidateRepository;
    private final ValidationVerdictRepository validationVerdictRepository;
    private final MarketBarRepository marketBarRepository;
    private final MarketSnapshotRepository marketSnapshotRepository;
    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;
    private final PersistenceMapper persistenceMapper;

    /**
     * Convenience overload using the STANDARD validation profile.
     */
    @Transactional
    public PipelineRunResult process(TradeCandidate candidate, List<MarketBar> bars) {
        return process(candidate, bars, ValidationProfile.STANDARD);
    }

    /**
     * Full pipeline with explicit validation profile.
     * If the candidate is already known (same candidateId), skips reprocessing and
     * returns the existing record flagged as a duplicate.
     */
    @Transactional
    @Timed(value = "reddragon.pipeline.candidate", description = "End-to-end candidate pipeline duration", histogram = true)
    public PipelineRunResult process(
            TradeCandidate candidate,
            List<MarketBar> bars,
            ValidationProfile profile
    ) {
        // Deduplication guard: skip the full pipeline for already-ingested candidates.
        if (candidateRepository.existsById(candidate.candidateId())) {
            return PipelineRunResult.duplicate(candidate);
        }

        ValidationService validationService = validationServiceFactory.forProfile(profile);

        // Feature extraction (L2) produces a raw snapshot with score=0
        // placeholders; the scoring step (L4) enriches it. See
        // lib-marketdata REVIEW.md Finding #8 for the architectural rationale.
        MarketDataSnapshot rawSnapshot = marketFeatureCalculator.process(candidate.symbol(), bars);
        MarketDataSnapshot marketData = marketDataSnapshotScorer.process(rawSnapshot);
        AnalyticsSnapshot analytics = analyticsService.process(candidate, marketData);

        ValidationAudit audit = validationService.process(
                validationInputFactory.process(candidate, marketData, analytics));
        ValidationResult validation = audit.validationResult();

        candidateRepository.save(persistenceMapper.toCandidateEntity(candidate));
        validationVerdictRepository.save(persistenceMapper.toValidationVerdictEntity(validation));
        persistNewBars(bars);
        marketSnapshotRepository.save(persistenceMapper.toMarketSnapshotEntity(candidate.candidateId(), marketData));
        analyticsSnapshotRepository.save(persistenceMapper.toAnalyticsSnapshotEntity(analytics));

        return PipelineRunResult.of(candidate, marketData, analytics, validation);
    }

    private void persistNewBars(List<MarketBar> bars) {
        List<MarketBar> newBars = bars.stream()
                .filter(bar -> !marketBarRepository.existsBySymbolAndBarDate(bar.symbol(), bar.date()))
                .toList();
        marketBarRepository.saveAll(persistenceMapper.toMarketBarEntities(newBars));
    }

}
