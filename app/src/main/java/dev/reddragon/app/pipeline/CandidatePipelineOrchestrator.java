package dev.reddragon.app.pipeline;

import dev.reddragon.analytics.model.AnalyticsSnapshot;
import dev.reddragon.analytics.service.DeterministicAnalyticsService;
import dev.reddragon.ingestion.model.TradeCandidate;
import dev.reddragon.marketdata.model.MarketBar;
import dev.reddragon.marketdata.model.MarketDataSnapshot;
import dev.reddragon.marketdata.service.MarketFeatureCalculator;
import dev.reddragon.persistence.mapper.PersistenceMapper;
import dev.reddragon.persistence.repository.AnalyticsSnapshotRepository;
import dev.reddragon.persistence.repository.CandidateRepository;
import dev.reddragon.persistence.repository.MarketBarRepository;
import dev.reddragon.persistence.repository.MarketSnapshotRepository;
import dev.reddragon.persistence.repository.ValidationVerdictRepository;
import dev.reddragon.validation.config.ValidationProfile;
import dev.reddragon.validation.model.CandidateValidationInput;
import dev.reddragon.validation.model.ValidationAudit;
import dev.reddragon.validation.model.ValidationResult;
import dev.reddragon.validation.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidatePipelineOrchestrator {

    private final MarketFeatureCalculator marketFeatureCalculator;
    private final DeterministicAnalyticsService analyticsService;
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

        MarketDataSnapshot marketData = marketFeatureCalculator.process(candidate.symbol(), bars);
        AnalyticsSnapshot analytics = analyticsService.process(candidate, marketData);

        CandidateValidationInput validationInput = validationInput(candidate, marketData, analytics);
        ValidationAudit audit = validationService.process(validationInput);
        ValidationResult validation = audit.validationResult();

        candidateRepository.save(persistenceMapper.toCandidateEntity(candidate));
        validationVerdictRepository.save(persistenceMapper.toValidationVerdictEntity(validation));
        marketBarRepository.saveAll(persistenceMapper.toMarketBarEntities(bars));
        marketSnapshotRepository.save(persistenceMapper.toMarketSnapshotEntity(candidate.candidateId(), marketData));
        analyticsSnapshotRepository.save(persistenceMapper.toAnalyticsSnapshotEntity(analytics));

        return PipelineRunResult.of(candidate, marketData, analytics, validation);
    }

    private CandidateValidationInput validationInput(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            AnalyticsSnapshot analytics
    ) {
        return new CandidateValidationInput(
                candidate.candidateId(),
                candidate.symbol(),
                candidate.structuralRealityScore(),
                candidate.materialSignificanceScore(),
                candidate.earlynessScore(),
                analytics.equilibriumQualityScore(),
                analytics.reflexivityPotentialScore(),
                analytics.asymmetryScore(),
                analytics.regimeCompatibilityScore(),
                analytics.deploymentConfidenceScore(),
                candidate.hasCredibleStructuralCatalyst(),
                marketData.complete(),
                candidate.earlynessScore() < 0.45,
                marketData.liquidityScore() < 0.35 || marketData.volatilityStabilityScore() < 0.35,
                marketData.rangePosition() > 0.90,
                candidate.summary()
        );
    }
}
