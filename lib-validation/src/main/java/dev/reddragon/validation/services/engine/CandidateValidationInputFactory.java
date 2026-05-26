package dev.reddragon.validation.services.engine;

import java.util.Objects;

import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.CandidateValidationInput;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.TradeCandidate;

/**
 * Builds the validation input used by both live pipeline processing and
 * backtests. Keeping these derived flags in one place prevents the live and
 * replay paths from drifting when thresholds or field mappings change.
 */
public class CandidateValidationInputFactory {

    private static final double EUPHORIC_EARLYNESS_THRESHOLD = 0.45;
    private static final double HOSTILE_LIQUIDITY_THRESHOLD = 0.35;
    private static final double HOSTILE_VOLATILITY_THRESHOLD = 0.35;
    private static final double REPRICED_RANGE_POSITION_THRESHOLD = 0.90;

    public CandidateValidationInput process(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            AnalyticsSnapshot analytics
    ) {
        Objects.requireNonNull(candidate, "candidate is required");
        Objects.requireNonNull(marketData, "marketData is required");
        Objects.requireNonNull(analytics, "analytics is required");

        return CandidateValidationInput.builder()
                .candidateId(candidate.candidateId())
                .symbol(candidate.symbol())
                .structuralRealityScore(candidate.structuralRealityScore())
                .materialSignificanceScore(candidate.materialSignificanceScore())
                .earlynessScore(candidate.earlynessScore())
                .equilibriumQualityScore(analytics.equilibriumQualityScore())
                .reflexivityPotentialScore(analytics.reflexivityPotentialScore())
                .asymmetryScore(analytics.asymmetryScore())
                .regimeCompatibilityScore(analytics.regimeCompatibilityScore())
                .deploymentConfidenceScore(analytics.deploymentConfidenceScore())
                .credibleCatalyst(candidate.hasCredibleStructuralCatalyst())
                .requiredDataPresent(marketData.complete())
                .euphoricOrSaturated(euphoricOrSaturated(candidate))
                .hostileMarketStructure(hostileMarketStructure(marketData))
                .equilibriumAlreadyRepriced(equilibriumAlreadyRepriced(marketData))
                .notes(candidate.summary())
                .build();
    }

    private boolean euphoricOrSaturated(TradeCandidate candidate) {
        return candidate.earlynessScore() < EUPHORIC_EARLYNESS_THRESHOLD;
    }

    private boolean hostileMarketStructure(MarketDataSnapshot marketData) {
        return marketData.liquidityScore() < HOSTILE_LIQUIDITY_THRESHOLD
                || marketData.volatilityStabilityScore() < HOSTILE_VOLATILITY_THRESHOLD;
    }

    private boolean equilibriumAlreadyRepriced(MarketDataSnapshot marketData) {
        return marketData.rangePosition() > REPRICED_RANGE_POSITION_THRESHOLD;
    }
}
