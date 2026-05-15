package dev.reddragon.analytics.services;

import dev.reddragon.analytics.models.AnalyticsSnapshot;
import dev.reddragon.analytics.models.PhaseLabel;
import dev.reddragon.analytics.models.PhaseTransitionSnapshot;
import dev.reddragon.analytics.models.RegimeLabel;
import dev.reddragon.analytics.services.propagation.PropagationPhaseAnalyzer;
import dev.reddragon.analytics.utilities.AnalyticsScoreUtils;
import dev.reddragon.ingestion.models.TradeCandidate;
import dev.reddragon.marketdata.models.MarketDataSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Produces deterministic analytics features from a candidate and market data.
 *
 * <p>Integrates {@link PropagationPhaseAnalyzer} to append a propagation-phase
 * note to the snapshot, enriching the trader review surface without requiring
 * a separate intraday data feed.
 */
public class DeterministicAnalyticsService {

    private final PropagationPhaseAnalyzer propagationPhaseAnalyzer = new PropagationPhaseAnalyzer();

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

        // Propagation phase — derived from reflexivity as a proxy for propagation level
        PhaseLabel phase = propagationPhase(candidate, reflexivity);
        notes.add("Propagation phase: " + phase.name());

        // Lightweight adversarial checks using available data
        adversarialNotes(candidate, marketData, reflexivity, notes);

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

    /**
     * Derives a {@link PhaseLabel} from the candidate's earlyness and reflexivity scores.
     * Uses {@link PropagationPhaseAnalyzer} with a synthetic {@link PhaseTransitionSnapshot}
     * computed from scores available in the current pipeline.
     */
    private PhaseLabel propagationPhase(TradeCandidate candidate, double currentReflexivity) {
        double previous = candidate.earlynessScore();
        double current  = currentReflexivity;
        double slope        = current - previous;
        double acceleration = slope > 0 ? slope * 0.5 : slope * 0.5; // simplified second derivative
        PhaseTransitionSnapshot snapshot = new PhaseTransitionSnapshot(previous, current, slope, acceleration);
        return propagationPhaseAnalyzer.process(snapshot);
    }

    /**
     * Appends lightweight adversarial observations when available signals suggest risk.
     * Full adversarial analysis requiring intraday/options snapshots is out of scope
     * for the standard pipeline run.
     */
    private void adversarialNotes(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            double reflexivity,
            List<String> notes
    ) {
        // Hype-without-structure signal
        if (candidate.structuralRealityScore() < 0.45 && reflexivity > 0.70) {
            notes.add("Adversarial flag: reflexivity is elevated but structural reality is weak; possible hype without substance.");
        }
        // Late-entry signal
        if (candidate.earlynessScore() < 0.40 && marketData.rangePosition() > 0.85) {
            notes.add("Adversarial flag: earlyness is low and price is near range high; late-entry risk elevated.");
        }
        // Liquidity deterioration
        if (marketData.liquidityScore() < 0.35 && marketData.volatilityStabilityScore() < 0.35) {
            notes.add("Adversarial flag: both liquidity and volatility stability are degraded; adverse execution risk.");
        }
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
        if (Math.abs(marketData.gapPercent()) > 0.15 && marketData.volatilityStabilityScore() < 0.50) {
            notes.add("Large opening displacement with unstable volatility; treating environment as news-driven and hostile to equilibrium assumptions.");
            return RegimeLabel.HOSTILE_NEWS_DRIVEN;
        }

        if (marketData.liquidityScore() < 0.35) {
            notes.add("Liquidity is weak; regime is hostile to concentration.");
            return RegimeLabel.HOSTILE_LIQUIDITY;
        }

        if (marketData.volatilityStabilityScore() < 0.35) {
            notes.add("Volatility is unstable; equilibrium behavior is degraded.");
            return RegimeLabel.HOSTILE_VOLATILITY;
        }

        if (marketData.rangePosition() >= 0.45
                && marketData.rangePosition() <= 0.55
                && marketData.volatilityStabilityScore() >= 0.70
                && marketData.liquidityScore() >= 0.60) {
            notes.add("Structure is tightly balanced with stable volatility and healthy liquidity; compression regime favorable for selective breakout monitoring.");
            return RegimeLabel.SUPPORTIVE_COMPRESSION;
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
            case SUPPORTIVE_COMPRESSION -> 0.72;
            case MIXED -> 0.50;
            case HOSTILE_NEWS_DRIVEN -> 0.22;
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
