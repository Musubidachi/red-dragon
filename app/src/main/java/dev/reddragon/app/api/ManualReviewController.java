package dev.reddragon.app.api;

import dev.reddragon.analytics.model.AnalyticsSnapshot;
import dev.reddragon.analytics.service.DeterministicAnalyticsService;
import dev.reddragon.ingestion.model.TradeCandidate;
import dev.reddragon.ingestion.service.ManualCandidateIngestionService;
import dev.reddragon.marketdata.model.MarketBar;
import dev.reddragon.marketdata.model.MarketDataSnapshot;
import dev.reddragon.marketdata.service.MarketFeatureCalculator;
import dev.reddragon.validation.engine.DisequilibriumValidationEngine;
import dev.reddragon.validation.model.CandidateValidationInput;
import dev.reddragon.validation.model.ValidationResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/review")
public class ManualReviewController {

    private final ManualCandidateIngestionService ingestionService;
    private final MarketFeatureCalculator marketFeatureCalculator;
    private final DeterministicAnalyticsService analyticsService;
    private final DisequilibriumValidationEngine validationEngine;

    public ManualReviewController(
            ManualCandidateIngestionService ingestionService,
            MarketFeatureCalculator marketFeatureCalculator,
            DeterministicAnalyticsService analyticsService,
            DisequilibriumValidationEngine validationEngine
    ) {
        this.ingestionService = ingestionService;
        this.marketFeatureCalculator = marketFeatureCalculator;
        this.analyticsService = analyticsService;
        this.validationEngine = validationEngine;
    }

    @PostMapping("/manual")
    public ManualReviewResponse reviewManualCandidate(@RequestBody ManualReviewRequest request) {
        TradeCandidate candidate = ingestionService.ingest(
                request.symbol(),
                request.companyName(),
                request.catalystType(),
                request.headline(),
                request.summary(),
                request.structuralRealityScore(),
                request.materialSignificanceScore(),
                request.earlynessScore(),
                request.reflexivityPotentialScore()
        );

        List<MarketBar> bars = request.bars() == null
                ? List.of()
                : request.bars().stream()
                .map(bar -> new MarketBar(
                        candidate.symbol(),
                        bar.date(),
                        bar.open(),
                        bar.high(),
                        bar.low(),
                        bar.close(),
                        bar.volume()
                ))
                .toList();

        MarketDataSnapshot marketData = marketFeatureCalculator.calculate(candidate.symbol(), bars);
        AnalyticsSnapshot analytics = analyticsService.analyze(candidate, marketData);
        ValidationResult validation = validationEngine.validate(validationInput(candidate, marketData, analytics));

        return new ManualReviewResponse(candidate, marketData, analytics, validation);
    }

    private CandidateValidationInput validationInput(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            AnalyticsSnapshot analytics
    ) {
        return CandidateValidationInput.builder()
                .candidateId(candidate.candidateId())
                .symbol(candidate.symbol())
                .structuralRealityScore(candidate.structuralRealityScore())
                .materialSignificanceScore(candidate.materialSignificanceScore())
                .earlynessScore(candidate.earlynessScore())
                .equilibriumQualityScore(analytics.equilibriumQualityScore())
                .reflexivityPotentialScore(analytics.reflexivityPotentialScore())
                .asymmetryScore(analytics.asymmetryScore())
                .regimeCompatibilityScore(analytics.regimeCompatibilityScore())
                .deploymentConfidenceScore(analytics.deploymentConfidenceScore())
                .credibleCatalyst(candidate.hasCredibleStructuralCatalyst())
                .requiredDataPresent(marketData.complete())
                .euphoricOrSaturated(candidate.earlynessScore() < 0.45)
                .hostileMarketStructure(marketData.liquidityScore() < 0.35 || marketData.volatilityStabilityScore() < 0.35)
                .equilibriumAlreadyRepriced(marketData.rangePosition() > 0.90)
                .notes(candidate.summary())
                .build();
    }
}
