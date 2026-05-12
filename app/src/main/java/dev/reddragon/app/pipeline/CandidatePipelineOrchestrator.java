package dev.reddragon.app.pipeline;

import dev.reddragon.analytics.model.AnalyticsSnapshot;
import dev.reddragon.analytics.service.DeterministicAnalyticsService;
import dev.reddragon.ingestion.model.TradeCandidate;
import dev.reddragon.marketdata.model.MarketBar;
import dev.reddragon.marketdata.model.MarketDataSnapshot;
import dev.reddragon.marketdata.service.MarketFeatureCalculator;
import dev.reddragon.persistence.mapper.PersistenceMapper;
import dev.reddragon.persistence.repository.CandidateRepository;
import dev.reddragon.persistence.repository.ValidationVerdictRepository;
import dev.reddragon.validation.engine.DisequilibriumValidationEngine;
import dev.reddragon.validation.model.CandidateValidationInput;
import dev.reddragon.validation.model.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidatePipelineOrchestrator {

    private final MarketFeatureCalculator marketFeatureCalculator;
    private final DeterministicAnalyticsService analyticsService;
    private final DisequilibriumValidationEngine validationEngine;
    private final CandidateRepository candidateRepository;
    private final ValidationVerdictRepository validationVerdictRepository;
    private final PersistenceMapper persistenceMapper;

    @Transactional
    public PipelineRunResult process(TradeCandidate candidate, List<MarketBar> bars) {
        MarketDataSnapshot marketData = marketFeatureCalculator.process(candidate.symbol(), bars);
        AnalyticsSnapshot analytics = analyticsService.process(candidate, marketData);
        ValidationResult validation = validationEngine.process(validationInput(candidate, marketData, analytics));

        candidateRepository.save(persistenceMapper.toCandidateEntity(candidate));
        validationVerdictRepository.save(persistenceMapper.toValidationVerdictEntity(validation));

        return new PipelineRunResult(candidate, marketData, analytics, validation);
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
