package dev.reddragon.app.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.reddragon.analytics.models.AnalyticsSnapshot;
import dev.reddragon.analytics.services.DeterministicAnalyticsService;
import dev.reddragon.app.models.ManualReviewRequest;
import dev.reddragon.app.models.ManualReviewResponse;
import dev.reddragon.ingestion.models.TradeCandidate;
import dev.reddragon.ingestion.services.ManualCandidateIngestionService;
import dev.reddragon.marketdata.models.MarketBar;
import dev.reddragon.marketdata.models.MarketDataSnapshot;
import dev.reddragon.marketdata.services.MarketFeatureCalculator;
import dev.reddragon.validation.models.CandidateValidationInput;
import dev.reddragon.validation.models.ValidationResult;
import dev.reddragon.validation.services.engine.DisequilibriumValidationEngine;
import lombok.RequiredArgsConstructor;

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
        TradeCandidate candidate = ingestionService.process(
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

        List<MarketBar> bars = marketBars(candidate, request);

        MarketDataSnapshot marketData = marketFeatureCalculator.process(candidate.symbol(), bars);
        AnalyticsSnapshot analytics = analyticsService.process(candidate, marketData);
        ValidationResult validation = validationEngine.process(validationInput(candidate, marketData, analytics));

        return new ManualReviewResponse(candidate, marketData, analytics, validation);
    }

    private List<MarketBar> marketBars(
            TradeCandidate candidate,
            ManualReviewRequest request
    ) {
        if (request.getBars() == null) {
            return List.of();
        }

        return request.getBars().stream()
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
