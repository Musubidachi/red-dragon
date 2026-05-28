package dev.reddragon.backtest.services;

import dev.reddragon.analytics.services.DeterministicAnalyticsService;
import dev.reddragon.analytics.services.ScoreResult;
import dev.reddragon.analytics.services.classification.VolatilityExpansionScorer;
import dev.reddragon.analytics.services.classification.VwapInteractionScorer;
import dev.reddragon.analytics.services.marketscoring.MarketDataSnapshotScorer;
import dev.reddragon.analytics.services.meta.LongHorizonCalibrationAnalyzer;
import dev.reddragon.backtest.models.BacktestFrame;
import dev.reddragon.backtest.models.BacktestOutcome;
import dev.reddragon.backtest.models.BacktestReport;
import dev.reddragon.domain.models.AnalyticsScoreBreakdown;
import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.CalibrationDriftLevel;
import dev.reddragon.domain.models.CalibrationReport;
import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.IntradayStructureSnapshot;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketIntradayStructureSnapshot;
import dev.reddragon.domain.models.MarketVolatilityExpansionSnapshot;
import dev.reddragon.domain.models.OutcomeSample;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.Verdict;
import dev.reddragon.domain.models.VolatilityExpansionSnapshot;
import dev.reddragon.marketdata.services.IntradayStructureSnapshotBuilder;
import dev.reddragon.marketdata.services.MarketFeatureCalculator;
import dev.reddragon.marketdata.services.VolatilityExpansionSnapshotBuilder;
import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.validation.services.engine.CandidateValidationInputFactory;
import dev.reddragon.validation.services.engine.DisequilibriumValidationEngine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoricalMarketDataCalibrationFixtureTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-04-01T13:30:00Z");
    private static final LocalDate DAILY_START = LocalDate.parse("2026-03-10");
    private static final double EPSILON = 1e-9;

    private final MarketFeatureCalculator marketFeatureCalculator = new MarketFeatureCalculator();
    private final MarketDataSnapshotScorer marketDataSnapshotScorer = new MarketDataSnapshotScorer();
    private final DeterministicAnalyticsService analyticsService = new DeterministicAnalyticsService();
    private final DisequilibriumValidationEngine validationEngine =
            new DisequilibriumValidationEngine(ValidationThresholds.defaults());
    private final CandidateValidationInputFactory validationInputFactory =
            new CandidateValidationInputFactory();
    private final BacktestReplayEngine backtestEngine = new BacktestReplayEngine(
            marketFeatureCalculator,
            marketDataSnapshotScorer,
            analyticsService,
            validationEngine,
            validationInputFactory
    );

    @Test
    void deterministicHistoricalFixturePropagatesWilderAtrThroughBacktestAndCalibration() {
        BacktestReport report = backtestEngine.process("rd-m5-historical-fixture", historicalFrames());

        assertEquals(3, report.metrics().totalFrames());
        assertEquals(1L, report.metrics().verdictCounts().get(Verdict.PASS));
        assertEquals(1L, report.metrics().verdictCounts().get(Verdict.WATCH));
        assertEquals(1L, report.metrics().verdictCounts().get(Verdict.REJECT));

        BacktestOutcome alpha = outcome(report, "rd-m5-alpha");
        assertEquals(3.0, alpha.marketData().averageTrueRange(), EPSILON);
        assertEquals(119.0, alpha.marketData().latestClose(), EPSILON);
        assertEquals(0.75, alpha.marketData().rangePosition(), EPSILON);
        assertEquals(MarketDataQuality.COMPLETE, alpha.marketData().quality());
        assertEquals(0.80, alpha.marketData().liquidityScore(), EPSILON);
        assertEquals(0.90, alpha.marketData().volatilityStabilityScore(), EPSILON);
        assertEquals(Verdict.PASS, alpha.validation().verdict());
        assertTrue(alpha.analytics().deploymentConfidenceScore() >= 0.80);

        BacktestOutcome bravo = outcome(report, "rd-m5-bravo");
        assertEquals(Verdict.WATCH, bravo.validation().verdict());
        assertEquals(16.0 / 7.0, bravo.marketData().averageTrueRange(), EPSILON);

        BacktestOutcome charlie = outcome(report, "rd-m5-charlie");
        assertEquals(Verdict.REJECT, charlie.validation().verdict());
        assertEquals(MarketDataQuality.ILLIQUID, charlie.marketData().quality());

        CalibrationReport calibration = new LongHorizonCalibrationAnalyzer().process(List.of(
                sample(alpha, 0.18, 0.04, true),
                sample(bravo, 0.09, 0.06, true),
                sample(charlie, -0.06, 0.14, false)
        ));

        assertEquals(CalibrationDriftLevel.STABLE, calibration.driftLevel());
        assertEquals(2.0 / 3.0, calibration.historicalWinRate(), EPSILON);
        assertEquals(0.07, calibration.averageReturn(), EPSILON);
        assertEquals(0.08, calibration.averageDrawdown(), EPSILON);
    }

    @Test
    void deterministicHistoricalFixturePinsSessionVwapAndRealizedVolatilityAnalyticsInputs() {
        MarketIntradayStructureSnapshot intraday =
                new IntradayStructureSnapshotBuilder().process(intradaySessionFixture());

        assertEquals(100.51666666666668, intraday.sessionVwap(), EPSILON);
        assertEquals((102.0 - 100.51666666666668) / 100.51666666666668,
                intraday.vwapDistancePercent(), EPSILON);
        assertEquals((102.0 - 100.51666666666668) / (102.2 - 100.4),
                intraday.vwapReclaimStrength(), EPSILON);
        assertTrue(intraday.aboveVwap());

        ScoreResult vwapScore = new VwapInteractionScorer().process(new IntradayStructureSnapshot(
                intraday.vwapDistancePercent(),
                intraday.vwapReclaimStrength(),
                intraday.directionalPersistenceScore(),
                intraday.rotationalQualityScore(),
                intraday.intradayTrendStrength(),
                intraday.aboveVwap()
        ));
        assertEquals(0.8008333333333334, vwapScore.score(), EPSILON);

        MarketVolatilityExpansionSnapshot volatility = new VolatilityExpansionSnapshotBuilder().process(
                "RDM5A",
                currentVolatilityFixture(),
                baselineVolatilityFixture()
        );

        assertEquals(2.0, volatility.currentAtr(), EPSILON);
        assertEquals(1.5, volatility.baselineAtr(), EPSILON);
        assertEquals(Math.sqrt(2.0) * Math.log(1.01), volatility.realizedVolatility(), EPSILON);
        assertEquals(0.50, volatility.volatilityExpansionScore(), EPSILON);
        assertEquals(0.50, volatility.volatilityCompressionScore(), EPSILON);

        ScoreResult volatilityScore = new VolatilityExpansionScorer().process(new VolatilityExpansionSnapshot(
                volatility.currentAtr() / 100.0,
                volatility.baselineAtr() / 100.0,
                0.30,
                volatility.volatilityExpansionScore(),
                volatility.volatilityCompressionScore()
        ));
        assertEquals(0.7525, volatilityScore.score(), EPSILON);
    }

    private List<BacktestFrame> historicalFrames() {
        return List.of(
                new BacktestFrame(
                        candidate("rd-m5-alpha", "RDM5A", 0.90, 0.85, 0.80, 0.78),
                        wilderDailyBars("RDM5A", 100.0, 16.0, 12.0, 1_200_000L)
                ),
                new BacktestFrame(
                        candidate("rd-m5-bravo", "RDM5B", 0.78, 0.70, 0.68, 0.62),
                        wilderDailyBars("RDM5B", 50.0, 6.0, 4.0, 1_500_000L)
                ),
                new BacktestFrame(
                        candidate("rd-m5-charlie", "RDM5C", 0.52, 0.50, 0.36, 0.72),
                        wilderDailyBars("RDM5C", 30.0, 24.0, 22.0, 80_000L)
                )
        );
    }

    private TradeCandidate candidate(
            String candidateId,
            String symbol,
            double structuralReality,
            double materialSignificance,
            double earlyness,
            double reflexivity
    ) {
        return TradeCandidate.builder()
                .candidateId(candidateId)
                .symbol(symbol)
                .companyName(symbol + " Historical Fixture Inc")
                .catalystType(CandidateCatalystType.GOVERNMENT_GRANT)
                .sourceType(SourceType.SEC_EDGAR)
                .observedAt(OBSERVED_AT)
                .headline("RD-M5 historical fixture " + symbol)
                .summary("Deterministic offline fixture for " + symbol)
                .structuralRealityScore(structuralReality)
                .materialSignificanceScore(materialSignificance)
                .earlynessScore(earlyness)
                .reflexivityPotentialScore(reflexivity)
                .build();
    }

    private List<MarketBar> wilderDailyBars(
            String symbol,
            double startClose,
            double finalTrueRange,
            double finalCloseOffset,
            long volume
    ) {
        List<MarketBar> bars = new ArrayList<>();
        bars.add(new MarketBar(
                symbol,
                DAILY_START,
                startClose - 0.50,
                startClose + 1.0,
                startClose - 1.0,
                startClose,
                volume
        ));

        double previousClose = startClose;
        for (int index = 1; index <= 14; index++) {
            double high = previousClose + 1.0;
            double low = previousClose - 1.0;
            double close = previousClose + 0.5;
            bars.add(new MarketBar(
                    symbol,
                    DAILY_START.plusDays(index),
                    previousClose,
                    high,
                    low,
                    close,
                    volume
            ));
            previousClose = close;
        }

        bars.add(new MarketBar(
                symbol,
                DAILY_START.plusDays(15),
                previousClose + 0.25,
                previousClose + finalTrueRange,
                previousClose,
                previousClose + finalCloseOffset,
                volume
        ));
        return bars;
    }

    private List<IntradayBar> intradaySessionFixture() {
        return List.of(
                intraday("RDM5A", "2026-03-31T14:30:00Z", 199.0, 201.0, 198.0, 200.0, 100_000L),
                intraday("RDM5A", "2026-04-01T13:30:00Z", 99.0, 100.0, 98.0, 99.0, 1_000L),
                intraday("RDM5A", "2026-04-01T13:35:00Z", 100.0, 101.0, 99.0, 100.0, 1_000L),
                intraday("RDM5A", "2026-04-01T13:40:00Z", 101.0, 102.2, 100.4, 102.0, 2_000L)
        );
    }

    private List<IntradayBar> currentVolatilityFixture() {
        return List.of(
                intraday("RDM5A", "2026-04-01T13:30:00Z", 100.0, 100.5, 99.5, 100.0, 1_000L),
                intraday("RDM5A", "2026-04-01T13:35:00Z", 100.0, 101.5, 99.5, 101.0, 1_000L),
                intraday("RDM5A", "2026-04-01T13:40:00Z", 101.0, 101.5, 99.5, 100.0, 1_000L)
        );
    }

    private List<IntradayBar> baselineVolatilityFixture() {
        return List.of(
                intraday("RDM5A", "2026-03-31T13:30:00Z", 100.0, 100.75, 99.25, 100.0, 1_000L),
                intraday("RDM5A", "2026-03-31T13:35:00Z", 100.0, 101.0, 99.5, 100.25, 1_000L),
                intraday("RDM5A", "2026-03-31T13:40:00Z", 100.25, 101.25, 99.75, 100.5, 1_000L)
        );
    }

    private IntradayBar intraday(
            String symbol,
            String startTime,
            double open,
            double high,
            double low,
            double close,
            long volume
    ) {
        return new IntradayBar(
                symbol,
                Instant.parse(startTime),
                open,
                high,
                low,
                close,
                volume,
                0.0
        );
    }

    private BacktestOutcome outcome(BacktestReport report, String candidateId) {
        return report.outcomes()
                .stream()
                .filter(outcome -> outcome.candidate().candidateId().equals(candidateId))
                .findFirst()
                .orElseThrow();
    }

    private OutcomeSample sample(
            BacktestOutcome outcome,
            double realizedReturn,
            double maxDrawdown,
            boolean thesisWorked
    ) {
        return new OutcomeSample(
                outcome.candidate().candidateId(),
                outcome.candidate().symbol(),
                outcome.analytics().observedAt(),
                scoreBreakdown(outcome),
                realizedReturn,
                maxDrawdown,
                12,
                thesisWorked
        );
    }

    private AnalyticsScoreBreakdown scoreBreakdown(BacktestOutcome outcome) {
        TradeCandidate candidate = outcome.candidate();
        AnalyticsSnapshot analytics = outcome.analytics();
        return new AnalyticsScoreBreakdown(
                candidate.structuralRealityScore(),
                candidate.materialSignificanceScore(),
                candidate.earlynessScore(),
                analytics.equilibriumQualityScore(),
                analytics.reflexivityPotentialScore(),
                analytics.asymmetryScore(),
                analytics.regimeCompatibilityScore(),
                analytics.deploymentConfidenceScore()
        );
    }
}
