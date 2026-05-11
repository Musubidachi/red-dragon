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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ManualReviewController {

    private final ManualCandidateIngestionService ingestionService;
    private final MarketFeatureCalculator marketFeatureCalculator;
    private final DeterministicAnalyticsService analyticsService;
    private final DisequilibriumValidationEngine validationEngine;

    @PostMapping("/manual")
    public ManualReviewResponse reviewManualCandidate(@RequestBody ManualReviewRequest request) {
        TradeCandidate candidate = ingestionService.ingest(
                request.getSymbol(),
                request.getCompanyName(),
                request.getCatalystType(),
                request.getHeadline(),
                request.getSummary(),
                request.getStructuralRealityScore(),
                request.getMaterialSignificanceScore(),
                request.getEarlynessScore(),
                request.getReflexivityPotentialScore()
        );

        List<MarketBar> bars = request.getBars() == null
                ? List.of()
                : request.getBars().stream()
                .map(bar -> new MarketBar(
                        candidate.symbol(),
                        bar.getDate(),
                        bar.getOpen(),
                        bar.getHigh(),
                        bar.getLow(),
                        bar.getClose(),
                        bar.getVolume()
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
