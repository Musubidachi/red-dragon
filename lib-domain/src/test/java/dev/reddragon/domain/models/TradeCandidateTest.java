package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the TradeCandidate value object - the record that every L1/L2
 * source produces and that flows through the rest of the pipeline.
 */
class TradeCandidateTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-05-13T00:00:00Z");

    @Test
    void builderProducesNormalizedCandidate() {
        TradeCandidate c = TradeCandidate.builder()
                .candidateId("abc-123")
                .symbol("  nvda ")
                .companyName("NVIDIA Corp")
                .catalystType(CandidateCatalystType.GOVERNMENT_GRANT)
                .sourceType(SourceType.SEC_EDGAR)
                .observedAt(OBSERVED_AT)
                .headline("Headline")
                .summary("Summary")
                .structuralRealityScore(0.80)
                .materialSignificanceScore(0.70)
                .earlynessScore(0.75)
                .reflexivityPotentialScore(0.55)
                .build();

        assertEquals("abc-123", c.candidateId());
        assertEquals("NVDA", c.symbol(), "symbol should be trimmed and upper-cased");
        assertEquals(CandidateCatalystType.GOVERNMENT_GRANT, c.catalystType());
        assertEquals(SourceType.SEC_EDGAR, c.sourceType());
    }

    @Test
    void hasCredibleStructuralCatalystUsesPointSixFiveThreshold() {
        TradeCandidate strong = candidateWithStructural(0.70);
        TradeCandidate weak   = candidateWithStructural(0.60);
        assertTrue(strong.hasCredibleStructuralCatalyst());
        assertFalse(weak.hasCredibleStructuralCatalyst());
    }

    @Test
    void nullSourceTypeDefaultsToManual() {
        TradeCandidate c = TradeCandidate.builder()
                .candidateId("x")
                .symbol("X")
                .catalystType(CandidateCatalystType.MANUAL_THESIS)
                .sourceType(null)
                .observedAt(OBSERVED_AT)
                .structuralRealityScore(0.5)
                .materialSignificanceScore(0.5)
                .earlynessScore(0.5)
                .reflexivityPotentialScore(0.5)
                .build();
        assertEquals(SourceType.MANUAL, c.sourceType());
    }

    @Test
    void outOfRangeStructuralRealityScoreThrows() {
        assertThrows(IllegalArgumentException.class, () -> candidateWithStructural(1.5));
        assertThrows(IllegalArgumentException.class, () -> candidateWithStructural(-0.1));
    }

    @Test
    void blankCandidateIdRejected() {
        assertThrows(IllegalArgumentException.class, () -> TradeCandidate.builder()
                .candidateId("   ")
                .symbol("X")
                .catalystType(CandidateCatalystType.MANUAL_THESIS)
                .observedAt(OBSERVED_AT)
                .structuralRealityScore(0.5)
                .materialSignificanceScore(0.5)
                .earlynessScore(0.5)
                .reflexivityPotentialScore(0.5)
                .build());
    }

    @Test
    void nullCatalystTypeRejected() {
        assertThrows(NullPointerException.class, () -> TradeCandidate.builder()
                .candidateId("x")
                .symbol("X")
                .catalystType(null)
                .observedAt(OBSERVED_AT)
                .structuralRealityScore(0.5)
                .materialSignificanceScore(0.5)
                .earlynessScore(0.5)
                .reflexivityPotentialScore(0.5)
                .build());
    }

    @Test
    void observedAtIsRequired() {
        assertThrows(NullPointerException.class, () -> TradeCandidate.builder()
                .candidateId("x")
                .symbol("X")
                .catalystType(CandidateCatalystType.MANUAL_THESIS)
                .structuralRealityScore(0.5)
                .materialSignificanceScore(0.5)
                .earlynessScore(0.5)
                .reflexivityPotentialScore(0.5)
                .build());
    }

    private TradeCandidate candidateWithStructural(double structural) {
        return TradeCandidate.builder()
                .candidateId("x")
                .symbol("X")
                .catalystType(CandidateCatalystType.FILING_EVENT)
                .observedAt(OBSERVED_AT)
                .structuralRealityScore(structural)
                .materialSignificanceScore(0.5)
                .earlynessScore(0.5)
                .reflexivityPotentialScore(0.5)
                .build();
    }
}
