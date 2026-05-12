package dev.reddragon.backtest.service;

import dev.reddragon.analytics.model.AnalyticsSnapshot;
import dev.reddragon.analytics.service.DeterministicAnalyticsService;
import dev.reddragon.backtest.model.BacktestFrame;
import dev.reddragon.backtest.model.BacktestMetrics;
import dev.reddragon.backtest.model.BacktestOutcome;
import dev.reddragon.backtest.model.BacktestReport;
import dev.reddragon.ingestion.model.TradeCandidate;
import dev.reddragon.marketdata.model.MarketDataSnapshot;
import dev.reddragon.marketdata.service.MarketFeatureCalculator;
import dev.reddragon.validation.engine.DisequilibriumValidationEngine;
import dev.reddragon.validation.model.CandidateValidationInput;
import dev.reddragon.validation.model.ValidationResult;

import java.util.List;

/**
 * Deterministic replay harness for historical candidates and historical bars.
 */
public class BacktestReplayEngine {

    private final MarketFeatureCalculator marketFeatureCalculator;
    private final DeterministicAnalyticsService analyticsService;
    private final DisequilibriumValidationEngine validationEngine;

    public BacktestReplayEngine(
            MarketFeatureCalculator marketFeatureCalculator,
            DeterministicAnalyticsService analyticsService,
            DisequilibriumValidationEngine validationEngine
    ) {
        this.marketFeatureCalculator = marketFeatureCalculator;
        this.analyticsService = analyticsService;
        this.validationEngine = validationEngine;
    }

    public BacktestReport process(String strategyName, List<BacktestFrame> frames) {
        List<BacktestOutcome> outcomes = (frames == null ? List.<BacktestFrame>of() : frames).stream()
                .map(this::replayFrame)
                .toList();
        return new BacktestReport(strategyName, BacktestMetrics.from(outcomes), outcomes);
    }

    private BacktestOutcome replayFrame(BacktestFrame frame) {
        TradeCandidate candidate = frame.candidate();
        MarketDataSnapshot marketData = marketFeatureCalculator.process(candidate.symbol(), frame.bars());
        AnalyticsSnapshot analytics = analyticsService.process(candidate, marketData);
        ValidationResult validation = validationEngine.process(validationInput(candidate, marketData, analytics));
        return new BacktestOutcome(candidate, marketData, analytics, validation);
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
