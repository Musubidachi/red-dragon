package dev.reddragon.backtest.models;

import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.DeploymentTier;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.RegimeLabel;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.ValidationResult;
import dev.reddragon.domain.models.Verdict;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BacktestMetricsTest {

    @Test
    void fromPinsVerdictDistributionAndAverageScore() {
        BacktestMetrics metrics = BacktestMetrics.from(List.of(
                outcome("pass", Verdict.PASS, 0.90),
                outcome("watch-a", Verdict.WATCH, 0.60),
                outcome("reject", Verdict.REJECT, 0.30),
                outcome("watch-b", Verdict.WATCH, 0.70)
        ));

        assertEquals(4, metrics.totalFrames());
        assertEquals(0.625, metrics.averageScore());
        assertEquals(Map.of(
                Verdict.PASS, 1L,
                Verdict.WATCH, 2L,
                Verdict.REJECT, 1L
        ), metrics.verdictCounts());
        assertEquals(0.25, metrics.passRate());
        assertEquals(0.50, metrics.watchRate());
        assertEquals(0.25, metrics.rejectRate());
    }

    private BacktestOutcome outcome(String candidateId, Verdict verdict, double score) {
        return new BacktestOutcome(
                candidate(candidateId),
                marketData(),
                analytics(candidateId),
                validation(candidateId, verdict, score)
        );
    }

    private TradeCandidate candidate(String candidateId) {
        return TradeCandidate.builder()
                .candidateId(candidateId)
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

    private MarketDataSnapshot marketData() {
        return new MarketDataSnapshot(
                "ACME",
                Instant.parse("2026-04-01T21:00:00Z"),
                101.0,
                100.0,
                0.01,
                1.20,
                0.50,
                1_000_000.0,
                0.80,
                0.75,
                1.0,
                0.0,
                0.60,
                MarketDataQuality.COMPLETE,
                List.of()
        );
    }

    private AnalyticsSnapshot analytics(String candidateId) {
        return new AnalyticsSnapshot(
                candidateId,
                "ACME",
                Instant.parse("2026-04-01T21:00:00Z"),
                RegimeLabel.SUPPORTIVE_ROTATIONAL,
                0.80,
                0.75,
                0.70,
                0.65,
                0.60,
                List.of()
        );
    }

    private ValidationResult validation(String candidateId, Verdict verdict, double score) {
        return new ValidationResult(
                candidateId,
                "ACME",
                verdict,
                verdict == Verdict.REJECT ? DeploymentTier.NONE : DeploymentTier.PROBE,
                score,
                List.of(),
                List.of(),
                List.of()
        );
    }
}
