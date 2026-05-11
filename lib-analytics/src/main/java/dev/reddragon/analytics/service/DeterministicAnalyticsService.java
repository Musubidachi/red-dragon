package dev.reddragon.analytics.service;

import dev.reddragon.analytics.model.AnalyticsSnapshot;
import dev.reddragon.analytics.model.RegimeLabel;
import dev.reddragon.analytics.util.AnalyticsScoreUtils;
import dev.reddragon.ingestion.model.TradeCandidate;
import dev.reddragon.marketdata.model.MarketDataSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Produces deterministic analytics features from a candidate and market data.
 */
public class DeterministicAnalyticsService {

    /**
     * Main processing flow.
     */
    public AnalyticsSnapshot process(
            TradeCandidate candidate,
            MarketDataSnapshot marketData
    ) {
        validate(candidate, marketData);

        List<String> notes = new ArrayList<>();

        RegimeLabel regime = regime(marketData, notes);
        double regimeCompatibility = regimeCompatibility(regime);
        double equilibriumQuality = equilibriumQuality(marketData);
        double asymmetry = asymmetry(candidate, marketData, equilibriumQuality, notes);
        double reflexivity = reflexivity(candidate);
        double deploymentConfidence = deploymentConfidence(
                candidate,
                asymmetry,
                regimeCompatibility
        );

        return buildSnapshot(
                candidate,
                notes,
                regime,
                regimeCompatibility,
                asymmetry,
                equilibriumQuality,
                reflexivity,
                deploymentConfidence
        );
    }

    private void validate(
            TradeCandidate candidate,
            MarketDataSnapshot marketData
    ) {
        if (candidate == null) {
            throw new IllegalArgumentException("candidate is required");
        }

        if (marketData == null) {
            throw new IllegalArgumentException("marketData is required");
        }
    }

    private double equilibriumQuality(MarketDataSnapshot marketData) {
        return AnalyticsScoreUtils.average(
                marketData.liquidityScore(),
                marketData.volatilityStabilityScore()
        );
    }

    private double reflexivity(TradeCandidate candidate) {
        return AnalyticsScoreUtils.clamp(
                AnalyticsScoreUtils.average(
                        candidate.reflexivityPotentialScore(),
                        candidate.earlynessScore()
                )
        );
    }

    private double deploymentConfidence(
            TradeCandidate candidate,
            double asymmetry,
            double regimeCompatibility
    ) {
        return AnalyticsScoreUtils.clamp(
                candidate.structuralRealityScore() * 0.25
                        + candidate.materialSignificanceScore() * 0.20
                        + candidate.earlynessScore() * 0.20
                        + asymmetry * 0.25
                        + regimeCompatibility * 0.10
        );
    }

    private AnalyticsSnapshot buildSnapshot(
            TradeCandidate candidate,
            List<String> notes,
            RegimeLabel regime,
            double regimeCompatibility,
            double asymmetry,
            double equilibriumQuality,
            double reflexivity,
            double deploymentConfidence
    ) {
        return new AnalyticsSnapshot(
                candidate.candidateId(),
                candidate.symbol(),
                Instant.now(),
                regime,
                regimeCompatibility,
                asymmetry,
                equilibriumQuality,
                reflexivity,
                deploymentConfidence,
                notes
        );
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

        if (marketData.rangePosition() > 0.75
                && marketData.volatilityStabilityScore() >= 0.60) {
            notes.add("Trend pressure is present but volatility remains controlled.");
            return RegimeLabel.SUPPORTIVE_TREND;
        }

        notes.add("Market regime is mixed; no hard support or rejection from market structure alone.");
        return RegimeLabel.MIXED;
    }

    private double regimeCompatibility(RegimeLabel regimeLabel) {
        return switch (regimeLabel) {
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
        double rangePenalty = rangePenalty(marketData, notes);
        double gapPenalty = gapPenalty(marketData, notes);

        return AnalyticsScoreUtils.clamp(
                candidate.structuralRealityScore() * 0.25
                        + candidate.materialSignificanceScore() * 0.25
                        + candidate.earlynessScore() * 0.25
                        + equilibriumQuality * 0.25
                        - rangePenalty
                        - gapPenalty
        );
    }

    private double rangePenalty(
            MarketDataSnapshot marketData,
            List<String> notes
    ) {
        if (marketData.rangePosition() > 0.85) {
            notes.add("Range position is extended; remaining asymmetry may be compressed.");
            return 0.20;
        }

        return 0.0;
    }

    private double gapPenalty(
            MarketDataSnapshot marketData,
            List<String> notes
    ) {
        if (Math.abs(marketData.gapPercent()) > 0.12) {
            notes.add("Large gap detected; entry asymmetry may be degraded.");
            return 0.15;
        }

        return 0.0;
    }
}
