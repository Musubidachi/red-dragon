package dev.reddragon.backtest.services;

import dev.reddragon.analytics.services.DeterministicAnalyticsService;
import dev.reddragon.analytics.services.marketscoring.MarketDataSnapshotScorer;
import dev.reddragon.backtest.models.BacktestFrame;
import dev.reddragon.backtest.models.BacktestOutcome;
import dev.reddragon.backtest.models.BacktestReport;
import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.CandidateValidationInput;
import dev.reddragon.domain.models.DeploymentTier;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.ValidationResult;
import dev.reddragon.domain.models.Verdict;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for BacktestReplayEngine - the public entry to lib-backtest.
 *
 * <p>The engine wires three real services (feature calculator, analytics,
 * validation), so these tests double as a thin integration smoke for the
 * full candidate pipeline running on historical bars.
 */
class BacktestReplayEngineTest {

    private final BacktestReplayEngine engine = new BacktestReplayEngine(
            new MarketFeatureCalculator(),
            new MarketDataSnapshotScorer(),
            new DeterministicAnalyticsService(),
            new DisequilibriumValidationEngine(ValidationThresholds.defaults()),
            new CandidateValidationInputFactory()
    );

    @Test
    void nullFrameListProducesEmptyReport() {
        BacktestReport report = engine.process("strategy", null);

        assertNotNull(report);
        assertEquals("strategy", report.strategyName());
        assertTrue(report.outcomes().isEmpty());
        assertEquals(0, report.metrics().totalFrames());
    }

    @Test
    void emptyFrameListProducesEmptyReport() {
        BacktestReport report = engine.process("strategy", List.of());
        assertEquals(0, report.metrics().totalFrames());
        assertEquals(0.0, report.metrics().averageScore());
    }

    @Test
    void singleFrameRunsAllThreePipelineStagesAndYieldsOneOutcome() {
        BacktestFrame frame = new BacktestFrame(sampleCandidate(), sampleBars(20));

        BacktestReport report = engine.process("smoke", List.of(frame));

        assertEquals(1, report.outcomes().size());
        BacktestOutcome outcome = report.outcomes().get(0);
        assertSame(frame.candidate(), outcome.candidate(), "candidate is passed through unchanged");
        assertNotNull(outcome.marketData(), "market-feature stage ran");
        assertNotNull(outcome.analytics(),  "analytics stage ran");
        assertNotNull(outcome.validation(), "validation stage ran");
        assertEquals(1, report.metrics().totalFrames());
    }

    @Test
    void validationStageUsesInjectedSharedInputFactoryOutput() {
        CandidateValidationInput factoryInput = CandidateValidationInput.builder()
                .candidateId("factory-input")
                .symbol("ACME")
                .structuralRealityScore(0.70)
                .materialSignificanceScore(0.70)
                .earlynessScore(0.70)
                .equilibriumQualityScore(0.70)
                .reflexivityPotentialScore(0.70)
                .asymmetryScore(0.70)
                .regimeCompatibilityScore(0.70)
                .deploymentConfidenceScore(0.70)
                .credibleCatalyst(true)
                .requiredDataPresent(true)
                .build();
        CapturingValidationInputFactory factory = new CapturingValidationInputFactory(factoryInput);
        CapturingValidationEngine validationEngine = new CapturingValidationEngine();
        BacktestReplayEngine engineWithCaptures = new BacktestReplayEngine(
                new MarketFeatureCalculator(),
                new MarketDataSnapshotScorer(),
                new DeterministicAnalyticsService(),
                validationEngine,
                factory
        );
        BacktestFrame frame = new BacktestFrame(sampleCandidate(), sampleBars(20));

        BacktestOutcome outcome = engineWithCaptures.process("shared-factory", List.of(frame))
                .outcomes()
                .get(0);

        assertEquals(1, factory.invocations);
        assertSame(frame.candidate(), factory.candidate);
        assertNotNull(factory.marketData, "factory received market data from replay");
        assertNotNull(factory.analytics, "factory received analytics from replay");
        assertSame(factoryInput, validationEngine.input);
        assertEquals("factory-input", outcome.validation().candidateId());
    }

    @Test
    void summaryFormatsAsOneLine() {
        BacktestFrame frame = new BacktestFrame(sampleCandidate(), sampleBars(20));
        BacktestReport report = engine.process("DisequilibriumV1", List.of(frame));

        String summary = report.summary();
        assertTrue(summary.startsWith("DisequilibriumV1:"), "summary should lead with strategy name");
        assertTrue(summary.contains("1 frames"), "summary should mention frame count");
    }

    private TradeCandidate sampleCandidate() {
        return TradeCandidate.builder()
                .candidateId("test-1")
                .symbol("ACME")
                .companyName("Acme Inc")
                .catalystType(CandidateCatalystType.GOVERNMENT_GRANT)
                .sourceType(SourceType.SEC_EDGAR)
                .observedAt(Instant.parse("2026-04-01T14:30:00Z"))
                .structuralRealityScore(0.80)
                .materialSignificanceScore(0.70)
                .earlynessScore(0.70)
                .reflexivityPotentialScore(0.55)
                .build();
    }

    private List<MarketBar> sampleBars(int count) {
        LocalDate start = LocalDate.parse("2026-04-01");
        List<MarketBar> bars = new ArrayList<>();
        IntStream.range(0, count).forEach(i -> {
            double base = 100.0 + i * 0.5;
            bars.add(new MarketBar(
                    "ACME",
                    start.plusDays(i),
                    base, base + 1.0, base - 1.0, base + 0.3,
                    1_000_000L
            ));
        });
        return bars;
    }

    private static final class CapturingValidationInputFactory extends CandidateValidationInputFactory {
        private final CandidateValidationInput factoryInput;
        private int invocations;
        private TradeCandidate candidate;
        private MarketDataSnapshot marketData;
        private AnalyticsSnapshot analytics;

        private CapturingValidationInputFactory(CandidateValidationInput factoryInput) {
            this.factoryInput = factoryInput;
        }

        @Override
        public CandidateValidationInput process(
                TradeCandidate candidate,
                MarketDataSnapshot marketData,
                AnalyticsSnapshot analytics
        ) {
            this.invocations++;
            this.candidate = candidate;
            this.marketData = marketData;
            this.analytics = analytics;
            return factoryInput;
        }
    }

    private static final class CapturingValidationEngine extends DisequilibriumValidationEngine {
        private CandidateValidationInput input;

        @Override
        public ValidationResult process(CandidateValidationInput input) {
            this.input = input;
            return new ValidationResult(
                    input.candidateId(),
                    input.symbol(),
                    Verdict.WATCH,
                    DeploymentTier.NONE,
                    0.42,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
    }
}
