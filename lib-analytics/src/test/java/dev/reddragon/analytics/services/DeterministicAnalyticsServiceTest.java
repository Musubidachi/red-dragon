package dev.reddragon.analytics.services;

import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for DeterministicAnalyticsService - the main lib-analytics entry
 * documented in the README's "Public API" section.
 */
class DeterministicAnalyticsServiceTest {

    private final DeterministicAnalyticsService service = new DeterministicAnalyticsService();

    @Test
    void nullCandidateRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.process(null, healthyMarketData("ACME")));
    }

    @Test
    void nullMarketDataRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.process(candidate(), null));
    }

    @Test
    void healthyInputsProduceCompleteSnapshot() {
        AnalyticsSnapshot snapshot = service.process(candidate(), healthyMarketData("ACME"));

        assertNotNull(snapshot);
        assertEquals("ACME", snapshot.symbol());
        // Every score must be in the documented 0.0-1.0 range
        assertTrue(in01(snapshot.regimeCompatibilityScore()));
        assertTrue(in01(snapshot.asymmetryScore()));
        assertTrue(in01(snapshot.equilibriumQualityScore()));
        assertTrue(in01(snapshot.reflexivityPotentialScore()));
        assertTrue(in01(snapshot.deploymentConfidenceScore()));
        assertTrue(snapshot.reasonNotes().stream().anyMatch(n -> n.startsWith("Propagation phase:")),
                "snapshot should include the propagation-phase note");
    }

    @Test
    void sameInputsProduceSameSnapshotSetOfScores() {
        TradeCandidate c = candidate();
        MarketDataSnapshot m = healthyMarketData("ACME");

        AnalyticsSnapshot a = service.process(c, m);
        AnalyticsSnapshot b = service.process(c, m);

        assertEquals(a.regimeLabel(), b.regimeLabel());
        assertEquals(a.asymmetryScore(), b.asymmetryScore(), 1e-9);
        assertEquals(a.equilibriumQualityScore(), b.equilibriumQualityScore(), 1e-9);
        assertEquals(a.deploymentConfidenceScore(), b.deploymentConfidenceScore(), 1e-9);
    }

    private boolean in01(double v) { return v >= 0.0 && v <= 1.0; }

    private TradeCandidate candidate() {
        return TradeCandidate.builder()
                .candidateId("c1")
                .symbol("ACME")
                .companyName("Acme Inc")
                .catalystType(CandidateCatalystType.GOVERNMENT_GRANT)
                .sourceType(SourceType.SEC_EDGAR)
                .structuralRealityScore(0.80)
                .materialSignificanceScore(0.70)
                .earlynessScore(0.75)
                .reflexivityPotentialScore(0.55)
                .build();
    }

    private MarketDataSnapshot healthyMarketData(String symbol) {
        return new MarketDataSnapshot(
                symbol, Instant.parse("2026-05-13T00:00:00Z"),
                100.0, 99.0, 0.01, 1.5, 0.5,
                1_000_000, 0.8, 0.7,
                1.2, 0.0, 0.55,
                MarketDataQuality.COMPLETE, List.of()
        );
    }
}
