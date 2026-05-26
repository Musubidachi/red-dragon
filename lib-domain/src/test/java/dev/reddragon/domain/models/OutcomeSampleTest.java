package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for OutcomeSample's returnCategory bucketing.
 */
class OutcomeSampleTest {

    private static final AnalyticsScoreBreakdown SCORES =
            new AnalyticsScoreBreakdown(0.7, 0.7, 0.7, 0.7, 0.7, 0.7, 0.7, 0.7);

    @Test
    void strongWinAboveTwentyPercent() {
        assertEquals("STRONG_WIN", sample(0.25).returnCategory());
    }

    @Test
    void winBetweenFiveAndTwentyPercent() {
        assertEquals("WIN", sample(0.10).returnCategory());
    }

    @Test
    void flatBetweenMinusFiveAndPlusFivePercent() {
        assertEquals("FLAT", sample(0.0).returnCategory());
        assertEquals("FLAT", sample(0.04).returnCategory());
        assertEquals("FLAT", sample(-0.04).returnCategory());
    }

    @Test
    void lossBetweenMinusFiveAndMinusTwentyPercent() {
        assertEquals("LOSS", sample(-0.15).returnCategory());
    }

    @Test
    void largeLossBelowMinusTwentyPercent() {
        assertEquals("LARGE_LOSS", sample(-0.25).returnCategory());
    }

    private OutcomeSample sample(double realizedReturn) {
        return new OutcomeSample("id", "ABC",
                Instant.parse("2026-05-13T00:00:00Z"),
                SCORES, realizedReturn, 0.1, 5, realizedReturn > 0);
    }
}
