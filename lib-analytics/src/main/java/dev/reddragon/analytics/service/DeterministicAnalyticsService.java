package dev.reddragon.analytics.service;

import dev.reddragon.analytics.model.AnalyticsSnapshot;
import dev.reddragon.analytics.model.RegimeLabel;
import dev.reddragon.ingestion.model.TradeCandidate;
import dev.reddragon.marketdata.model.MarketDataSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based analytics baseline.
 *
 * This does not attempt prediction. It translates candidate and market-data
 * context into the normalized dimensions consumed by validation.
 */
public class DeterministicAnalyticsService {

    public AnalyticsSnapshot analyze(TradeCandidate candidate, MarketDataSnapshot marketData) {
        if (candidate == null) {
            throw new IllegalArgumentException("candidate is required");
        }
        if (marketData == null) {
            throw new IllegalArgumentException("marketData is required");
        }

        List<String> notes = new ArrayList<>();
        RegimeLabel regime = regime(marketData, notes);
        double regimeCompatibility = regimeCompatibility(regime);
        double equilibriumQuality = average(marketData.liquidityScore(), marketData.volatilityStabilityScore());
        double asymmetry = asymmetry(candidate, marketData, equilibriumQuality, notes);
        double reflexivity = clamp(average(candidate.reflexivityPotentialScore(), candidate.earlynessScore()));
        double deploymentConfidence = clamp(
                candidate.structuralRealityScore() * 0.25
                        + candidate.materialSignificanceScore() * 0.20
                        + candidate.earlynessScore() * 0.20
                        + asymmetry * 0.25
                        + regimeCompatibility * 0.10
        );

        return AnalyticsSnapshot.builder()
                .candidateId(candidate.candidateId())
                .symbol(candidate.symbol())
                .observedAt(Instant.now())
                .regimeLabel(regime)
                .regimeCompatibilityScore(regimeCompatibility)
                .asymmetryScore(asymmetry)
                .equilibriumQualityScore(equilibriumQuality)
                .reflexivityPotentialScore(reflexivity)
                .deploymentConfidenceScore(deploymentConfidence)
                .reasonNotes(notes)
                .build();
    }

    private RegimeLabel regime(MarketDataSnapshot marketData, List<String> notes) {
        if (marketData.liquidityScore() < 0.35) {
            notes.add("Liquidity is weak; regime is hostile to concentration.");
            return RegimeLabel.HOSTILE_LIQUIDITY;
        }
        if (marketData.volatilityStabilityScore() < 0.35) {
            notes.add("Volatility is unstable; equilibrium behavior is degraded.");
            return RegimeLabel.HOSTILE_VOLATILITY;
        }
        if (marketData.rangePosition() >= 0.35 && marketData.rangePosition() <= 0.75) {
            notes.add("Range position is balanced enough for rotational/restoration behavior.");
            return RegimeLabel.SUPPORTIVE_ROTATIONAL;
        }
        if (marketData.rangePosition() > 0.75 && marketData.volatilityStabilityScore() >= 0.60) {
            notes.add("Trend pressure is present but volatility remains controlled.");
            return RegimeLabel.SUPPORTIVE_TREND;
        }
        notes.add("Market regime is mixed; no hard support or rejection from market structure alone.");
        return RegimeLabel.MIXED;
    }

    private double regimeCompatibility(RegimeLabel label) {
        return switch (label) {
            case SUPPORTIVE_ROTATIONAL -> 0.85;
            case SUPPORTIVE_TREND -> 0.70;
            case MIXED -> 0.50;
            case HOSTILE_VOLATILITY -> 0.25;
            case HOSTILE_LIQUIDITY -> 0.20;
        };
    }

    private double asymmetry(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            double equilibriumQuality,
            List<String> notes
    ) {
        double earlyness = candidate.earlynessScore();
        double materiality = candidate.materialSignificanceScore();
        double structuralReality = candidate.structuralRealityScore();
        double rangePenalty = marketData.rangePosition() > 0.85 ? 0.20 : 0.0;
        double gapPenalty = Math.abs(marketData.gapPercent()) > 0.12 ? 0.15 : 0.0;

        if (rangePenalty > 0.0) {
            notes.add("Range position is extended; remaining asymmetry may be compressed.");
        }
        if (gapPenalty > 0.0) {
            notes.add("Large gap detected; entry asymmetry may be degraded.");
        }

        return clamp(
                structuralReality * 0.25
                        + materiality * 0.25
                        + earlyness * 0.25
                        + equilibriumQuality * 0.25
                        - rangePenalty
                        - gapPenalty
        );
    }

    private double average(double left, double right) {
        return (left + right) / 2.0;
    }

    private double clamp(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }
}
