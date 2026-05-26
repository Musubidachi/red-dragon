package dev.reddragon.backtest.services;

import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.analytics.services.DeterministicAnalyticsService;
import dev.reddragon.analytics.services.marketscoring.MarketDataSnapshotScorer;
import dev.reddragon.backtest.models.BacktestFrame;
import dev.reddragon.backtest.models.BacktestMetrics;
import dev.reddragon.backtest.models.BacktestOutcome;
import dev.reddragon.backtest.models.BacktestReport;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.marketdata.services.MarketFeatureCalculator;
import dev.reddragon.validation.services.engine.DisequilibriumValidationEngine;
import dev.reddragon.domain.models.ValidationResult;
import dev.reddragon.validation.services.engine.CandidateValidationInputFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * Deterministic replay harness for historical candidates and historical bars.
 */
@RequiredArgsConstructor
public class BacktestReplayEngine {

    private final MarketFeatureCalculator marketFeatureCalculator;
    private final MarketDataSnapshotScorer marketDataSnapshotScorer;
    private final DeterministicAnalyticsService analyticsService;
    private final DisequilibriumValidationEngine validationEngine;
    private final CandidateValidationInputFactory validationInputFactory;

    public BacktestReport process(String strategyName, List<BacktestFrame> frames) {
        Objects.requireNonNull(strategyName, "strategyName is required");
        List<BacktestOutcome> outcomes = (frames == null ? List.<BacktestFrame>of() : frames).stream()
                .map(this::replayFrame)
                .toList();
        return new BacktestReport(strategyName, BacktestMetrics.from(outcomes), outcomes);
    }

    private BacktestOutcome replayFrame(BacktestFrame frame) {
        TradeCandidate candidate = frame.candidate();
        // Raw L2 snapshot is enriched with liquidity and volatility
        // scores. See lib-marketdata REVIEW.md Finding #8.
        MarketDataSnapshot rawSnapshot = marketFeatureCalculator.process(candidate.symbol(), frame.bars());
        MarketDataSnapshot marketData = marketDataSnapshotScorer.process(rawSnapshot);
        AnalyticsSnapshot analytics = analyticsService.process(candidate, marketData);
        ValidationResult validation = validationEngine.process(
                validationInputFactory.process(candidate, marketData, analytics));
        return new BacktestOutcome(candidate, marketData, analytics, validation);
    }
}
