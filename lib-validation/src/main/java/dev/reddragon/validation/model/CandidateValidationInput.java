package dev.reddragon.validation.model;

import java.util.Objects;

/**
 * Normalized validation input for one trade candidate.
 *
 * This model intentionally represents your trading process as normalized
 * evidence scores rather than raw market/provider objects. Upstream modules
 * can evolve independently as long as they can produce these scores.
 */
public record CandidateValidationInput(
        String candidateId,
        String symbol,

        /** Credibility/objectivity of the catalyst or structural change. */
        double structuralRealityScore,

        /** Materiality of the catalyst relative to the company, sector, and capital-flow impact. */
        double materialSignificanceScore,

        /** How early the narrative/equilibrium shift appears to be. Higher means earlier and less saturated. */
        double earlynessScore,

        /** Quality of rotational/restoration structure, liquidity stability, and volatility behavior. */
        double equilibriumQualityScore,

        /** Probability that real structural change becomes socially/market amplified. */
        double reflexivityPotentialScore,

        /** Favorability of payoff distribution after current price movement. */
        double asymmetryScore,

        /** Compatibility of the broader regime with this framework. */
        double regimeCompatibilityScore,

        /** Whether the candidate deserves aggressive capital review after all prior validation. */
        double deploymentConfidenceScore,

        /** True when the catalyst is based on credible objective information rather than hype alone. */
        boolean credibleCatalyst,

        /** True when required source, market-data, and analytics inputs are present. */
        boolean requiredDataPresent,

        /** True when narrative/social propagation is euphoric or fully saturated. */
        boolean euphoricOrSaturated,

        /** True when liquidity, spread, or volatility conditions are hostile enough to block review. */
        boolean hostileMarketStructure,

        /** True when the move appears already fully repriced and asymmetry is gone. */
        boolean equilibriumAlreadyRepriced,

        String notes
) {
    public CandidateValidationInput {
        candidateId = normalize(candidateId);
        symbol = normalize(symbol);
        notes = notes == null ? "" : notes.trim();
        Objects.requireNonNull(candidateId, "candidateId is required");
        Objects.requireNonNull(symbol, "symbol is required");

        structuralRealityScore = requireNormalized("structuralRealityScore", structuralRealityScore);
        materialSignificanceScore = requireNormalized("materialSignificanceScore", materialSignificanceScore);
        earlynessScore = requireNormalized("earlynessScore", earlynessScore);
        equilibriumQualityScore = requireNormalized("equilibriumQualityScore", equilibriumQualityScore);
        reflexivityPotentialScore = requireNormalized("reflexivityPotentialScore", reflexivityPotentialScore);
        asymmetryScore = requireNormalized("asymmetryScore", asymmetryScore);
        regimeCompatibilityScore = requireNormalized("regimeCompatibilityScore", regimeCompatibilityScore);
        deploymentConfidenceScore = requireNormalized("deploymentConfidenceScore", deploymentConfidenceScore);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static double requireNormalized(String fieldName, double value) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
        return value;
    }
}
