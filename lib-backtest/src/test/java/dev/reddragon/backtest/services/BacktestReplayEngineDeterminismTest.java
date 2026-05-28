package dev.reddragon.backtest.services;

import dev.reddragon.analytics.services.DeterministicAnalyticsService;
import dev.reddragon.analytics.services.marketscoring.MarketDataSnapshotScorer;
import dev.reddragon.backtest.models.BacktestFrame;
import dev.reddragon.backtest.models.BacktestOutcome;
import dev.reddragon.backtest.models.BacktestReport;
import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.CandidateValidationInput;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.ValidationResult;
import dev.reddragon.marketdata.services.MarketFeatureCalculator;
import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.validation.services.engine.CandidateValidationInputFactory;
import dev.reddragon.validation.services.engine.DisequilibriumValidationEngine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BacktestReplayEngineDeterminismTest {

    private final MarketFeatureCalculator marketFeatureCalculator = new MarketFeatureCalculator();
    private final MarketDataSnapshotScorer marketDataSnapshotScorer = new MarketDataSnapshotScorer();
    private final DeterministicAnalyticsService analyticsService = new DeterministicAnalyticsService();
    private final DisequilibriumValidationEngine validationEngine =
            new DisequilibriumValidationEngine(ValidationThresholds.defaults());
    private final CandidateValidationInputFactory validationInputFactory =
            new CandidateValidationInputFactory();
    private final BacktestReplayEngine engine = new BacktestReplayEngine(
            marketFeatureCalculator,
            marketDataSnapshotScorer,
            analyticsService,
            validationEngine,
            validationInputFactory
    );

    @Test
    void repeatedReplayOfSameFramesProducesEqualReport() {
        List<BacktestFrame> frames = replayFrames();

        BacktestReport first = engine.process("deterministic-fixture", frames);
        BacktestReport second = engine.process("deterministic-fixture", frames);

        assertEquals(first, second);
        assertEquals(first.summary(), second.summary());
    }

    @Test
    void replayOutcomeMatchesDirectServicePipelineForEachFrame() {
        List<BacktestFrame> frames = replayFrames();

        BacktestReport report = engine.process("direct-equivalence", frames);

        assertEquals(frames.size(), report.outcomes().size());
        for (int i = 0; i < frames.size(); i++) {
            assertEquals(directOutcome(frames.get(i)), report.outcomes().get(i));
        }
    }

    private BacktestOutcome directOutcome(BacktestFrame frame) {
        TradeCandidate candidate = frame.candidate();
        MarketDataSnapshot rawMarketData = marketFeatureCalculator.process(candidate.symbol(), frame.bars());
        MarketDataSnapshot marketData = marketDataSnapshotScorer.process(rawMarketData);
        AnalyticsSnapshot analytics = analyticsService.process(candidate, marketData);
        CandidateValidationInput validationInput = validationInputFactory.process(candidate, marketData, analytics);
        ValidationResult validation = validationEngine.process(validationInput);
        return new BacktestOutcome(candidate, marketData, analytics, validation);
    }

    private List<BacktestFrame> replayFrames() {
        return List.of(
                new BacktestFrame(
                        candidate("fixture-pass", "ACME", 0.90, 0.82, 0.82, 0.78),
                        bars("ACME", 96.0, 0.65, 1_500_000L)
                ),
                new BacktestFrame(
                        candidate("fixture-watch", "BRAV", 0.74, 0.62, 0.62, 0.52),
                        bars("BRAV", 42.0, 0.25, 800_000L)
                ),
                new BacktestFrame(
                        candidate("fixture-reject", "CHAR", 0.40, 0.32, 0.30, 0.76),
                        bars("CHAR", 18.0, 0.90, 60_000L)
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
                .companyName(symbol + " Inc")
                .catalystType(CandidateCatalystType.GOVERNMENT_GRANT)
                .sourceType(SourceType.SEC_EDGAR)
                .observedAt(Instant.parse("2026-04-01T14:30:00Z"))
                .headline("Historic catalyst for " + symbol)
                .summary("Replay fixture for " + symbol)
                .structuralRealityScore(structuralReality)
                .materialSignificanceScore(materialSignificance)
                .earlynessScore(earlyness)
                .reflexivityPotentialScore(reflexivity)
                .build();
    }

    private List<MarketBar> bars(String symbol, double startClose, double dailyStep, long baseVolume) {
        LocalDate start = LocalDate.parse("2026-03-01");
        List<MarketBar> bars = new ArrayList<>();
        IntStream.range(0, 20).forEach(i -> {
            double close = startClose + i * dailyStep;
            bars.add(new MarketBar(
                    symbol,
                    start.plusDays(i),
                    close - 0.20,
                    close + 0.80,
                    close - 0.90,
                    close,
                    baseVolume + i * 10_000L
            ));
        });
        return bars;
    }
}
