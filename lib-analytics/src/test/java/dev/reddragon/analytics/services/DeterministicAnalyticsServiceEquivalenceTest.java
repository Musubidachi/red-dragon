package dev.reddragon.analytics.services;

import dev.reddragon.analytics.services.classification.EquilibriumQualityScorer;
import dev.reddragon.analytics.services.classification.RegimeCompatibilityResult;
import dev.reddragon.analytics.services.classification.RegimeCompatibilityScorer;
import dev.reddragon.analytics.services.deployment.DeploymentConfidenceScorer;
import dev.reddragon.analytics.services.propagation.PropagationPhaseAnalyzer;
import dev.reddragon.analytics.services.propagation.ReflexivityScorer;
import dev.reddragon.analytics.services.structural.AsymmetryScorer;
import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.MarketIntradayStructureSnapshot;
import dev.reddragon.domain.models.MarketStateSignal;
import dev.reddragon.domain.models.PhaseLabel;
import dev.reddragon.domain.models.PhaseTransitionSnapshot;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicAnalyticsServiceEquivalenceTest {

    private static final double EPSILON = 1e-9;
    private static final Instant T0 = Instant.parse("2026-05-13T00:00:00Z");

    private final DeterministicAnalyticsService service = new DeterministicAnalyticsService();
    private final RegimeCompatibilityScorer regimeCompatibilityScorer = new RegimeCompatibilityScorer();
    private final EquilibriumQualityScorer equilibriumQualityScorer = new EquilibriumQualityScorer();
    private final AsymmetryScorer asymmetryScorer = new AsymmetryScorer();
    private final ReflexivityScorer reflexivityScorer = new ReflexivityScorer();
    private final DeploymentConfidenceScorer deploymentConfidenceScorer = new DeploymentConfidenceScorer();
    private final PropagationPhaseAnalyzer propagationPhaseAnalyzer = new PropagationPhaseAnalyzer();
    private final MarketStateClassifier marketStateClassifier = new MarketStateClassifier();

    @Test
    void processMatchesScorerPipelineForRepresentativeInputs() {
        assertSnapshotMatchesScorers(
                "supportive compression",
                candidate("compression", 0.80, 0.70, 0.75, 0.55),
                marketData("ACME", T0, 0.01, 0.50, 0.80, 0.80)
        );
        assertSnapshotMatchesScorers(
                "supportive trend",
                candidate("trend", 0.90, 0.80, 0.30, 0.90),
                marketData("TREND", T0.plusSeconds(60), 0.02, 0.82, 0.75, 0.70)
        );
        assertSnapshotMatchesScorers(
                "hostile news driven",
                candidate("gap", 0.30, 0.60, 0.30, 0.90),
                marketData("GAPS", T0.plusSeconds(120), 0.20, 0.90, 0.80, 0.40)
        );
    }

    @Test
    void processRegimeMatchesStandaloneClassifierForSharedRegimeLabels() {
        assertRegimeMatchesStandaloneClassifier(
                "supportive rotational",
                marketData("ROTA", T0, 0.01, 0.60, 0.80, 0.60),
                intraday("ROTA", 0.80, 0.30, 0.40, 0.50, 0.50)
        );
        assertRegimeMatchesStandaloneClassifier(
                "supportive trend",
                marketData("STRN", T0, 0.01, 0.82, 0.80, 0.70),
                intraday("STRN", 0.30, 0.85, 0.80, 0.50, 0.50)
        );
        assertRegimeMatchesStandaloneClassifier(
                "hostile volatility",
                marketData("HVOL", T0, 0.01, 0.90, 0.80, 0.20),
                intraday("HVOL", 0.40, 0.40, 0.40, 0.15, 3.00)
        );
        assertRegimeMatchesStandaloneClassifier(
                "hostile liquidity",
                marketData("HLIQ", T0, 0.01, 0.60, 0.20, 0.80),
                intraday("HLIQ", 0.15, 0.20, 0.20, 0.50, 0.50)
        );
        assertRegimeMatchesStandaloneClassifier(
                "mixed",
                marketData("MIXD", T0, 0.01, 0.90, 0.80, 0.50),
                intraday("MIXD", 0.50, 0.50, 0.50, 0.50, 0.50)
        );
    }

    private void assertSnapshotMatchesScorers(
            String label,
            TradeCandidate candidate,
            MarketDataSnapshot marketData
    ) {
        RegimeCompatibilityResult regimeResult = regimeCompatibilityScorer.process(marketData);
        double regimeCompatibility = regimeCompatibilityScorer.score(regimeResult.regimeLabel());
        ScoreResult equilibriumQualityResult = equilibriumQualityScorer.process(marketData);
        ScoreResult asymmetryResult = asymmetryScorer.process(candidate, marketData, equilibriumQualityResult.score());
        ScoreResult reflexivityResult = reflexivityScorer.process(candidate);
        ScoreResult deploymentConfidenceResult = deploymentConfidenceScorer.process(
                candidate,
                asymmetryResult.score(),
                equilibriumQualityResult.score(),
                regimeCompatibility,
                reflexivityResult.score()
        );
        PhaseLabel phase = propagationPhase(candidate, reflexivityResult.score());

        AnalyticsSnapshot snapshot = service.process(candidate, marketData);

        assertEquals(candidate.candidateId(), snapshot.candidateId(), label + ": candidate id");
        assertEquals(candidate.symbol(), snapshot.symbol(), label + ": symbol");
        assertEquals(marketData.observedAt(), snapshot.observedAt(), label + ": observation timestamp");
        assertEquals(regimeResult.regimeLabel(), snapshot.regimeLabel(), label + ": regime");
        assertEquals(regimeCompatibility, snapshot.regimeCompatibilityScore(), EPSILON, label + ": regime score");
        assertEquals(asymmetryResult.score(), snapshot.asymmetryScore(), EPSILON, label + ": asymmetry score");
        assertEquals(equilibriumQualityResult.score(), snapshot.equilibriumQualityScore(), EPSILON, label + ": equilibrium quality");
        assertEquals(reflexivityResult.score(), snapshot.reflexivityPotentialScore(), EPSILON, label + ": reflexivity");
        assertEquals(deploymentConfidenceResult.score(), snapshot.deploymentConfidenceScore(), EPSILON, label + ": deployment confidence");
        assertNotesIncluded(label, snapshot.reasonNotes(), regimeResult.notes());
        assertNotesIncluded(label, snapshot.reasonNotes(), equilibriumQualityResult.notes());
        assertNotesIncluded(label, snapshot.reasonNotes(), asymmetryResult.notes());
        assertNotesIncluded(label, snapshot.reasonNotes(), reflexivityResult.notes());
        assertNotesIncluded(label, snapshot.reasonNotes(), deploymentConfidenceResult.notes());
        assertTrue(
                snapshot.reasonNotes().contains("Propagation phase: " + phase.name()),
                label + ": propagation phase note should match PropagationPhaseAnalyzer"
        );
    }

    private void assertRegimeMatchesStandaloneClassifier(
            String label,
            MarketDataSnapshot marketData,
            MarketIntradayStructureSnapshot intraday
    ) {
        AnalyticsSnapshot snapshot = service.process(candidate(label, 0.80, 0.70, 0.75, 0.55), marketData);
        RegimeCompatibilityResult regimeResult = regimeCompatibilityScorer.process(marketData);
        MarketStateSignal standaloneSignal = marketStateClassifier.process(intraday);

        assertEquals(regimeResult.regimeLabel(), snapshot.regimeLabel(), label + ": scorer/orchestrator regime");
        assertEquals(standaloneSignal.regimeLabel(), snapshot.regimeLabel(), label + ": standalone classifier regime");
    }

    private PhaseLabel propagationPhase(TradeCandidate candidate, double reflexivity) {
        double slope = reflexivity - candidate.earlynessScore();
        double acceleration = slope * 0.5;
        return propagationPhaseAnalyzer.process(new PhaseTransitionSnapshot(
                candidate.earlynessScore(),
                reflexivity,
                slope,
                acceleration
        ));
    }

    private void assertNotesIncluded(String label, List<String> actualNotes, List<String> expectedNotes) {
        assertTrue(
                actualNotes.containsAll(expectedNotes),
                label + ": orchestrator should include scorer notes " + expectedNotes
        );
    }

    private TradeCandidate candidate(
            String id,
            double structural,
            double material,
            double earlyness,
            double reflexivity
    ) {
        String slug = id.replace(" ", "-");
        return TradeCandidate.builder()
                .candidateId("candidate-" + slug)
                .symbol("ACME")
                .companyName("Acme Inc")
                .catalystType(CandidateCatalystType.GOVERNMENT_GRANT)
                .sourceType(SourceType.SEC_EDGAR)
                .sourceId("accession-" + slug)
                .sourceUrl("https://example.com/" + slug)
                .observedAt(T0)
                .headline("Grant awarded")
                .summary("Award summary")
                .structuralRealityScore(structural)
                .materialSignificanceScore(material)
                .earlynessScore(earlyness)
                .reflexivityPotentialScore(reflexivity)
                .build();
    }

    private MarketDataSnapshot marketData(
            String symbol,
            Instant observedAt,
            double gapPercent,
            double rangePosition,
            double liquidity,
            double volatility
    ) {
        return new MarketDataSnapshot(
                symbol,
                observedAt,
                10.0,
                9.8,
                gapPercent,
                0.20,
                rangePosition,
                1_000_000,
                liquidity,
                volatility,
                1.20,
                0.00,
                0.50,
                MarketDataQuality.COMPLETE,
                List.of()
        );
    }

    private MarketIntradayStructureSnapshot intraday(
            String symbol,
            double rotational,
            double persistence,
            double trend,
            double reclaim,
            double vwapDistancePercent
    ) {
        return new MarketIntradayStructureSnapshot(
                symbol,
                100.0,
                100.0,
                vwapDistancePercent,
                reclaim,
                persistence,
                rotational,
                trend,
                true
        );
    }
}
