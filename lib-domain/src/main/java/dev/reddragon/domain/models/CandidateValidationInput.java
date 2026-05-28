package dev.reddragon.domain.models;

import dev.reddragon.domain.utilities.DomainScorePolicy;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * Normalized validation input for one trade candidate.
 *
 * This model intentionally represents your trading process as normalized
 * evidence scores rather than raw market/provider objects. Upstream modules
 * can evolve independently as long as they can produce these scores.
 */
@Value
@Accessors(fluent = true)
public class CandidateValidationInput {
    String candidateId;
    String symbol;

    /** Credibility/objectivity of the catalyst or structural change. */
    double structuralRealityScore;

    /** Materiality of the catalyst relative to the company, sector, and capital-flow impact. */
    double materialSignificanceScore;

    /** How early the narrative/equilibrium shift appears to be. Higher means earlier and less saturated. */
    double earlynessScore;

    /** Quality of rotational/restoration structure, liquidity stability, and volatility behavior. */
    double equilibriumQualityScore;

    /** Probability that real structural change becomes socially/market amplified. */
    double reflexivityPotentialScore;

    /** Favorability of payoff distribution after current price movement. */
    double asymmetryScore;

    /** Compatibility of the broader regime with this framework. */
    double regimeCompatibilityScore;

    /** Whether the candidate deserves aggressive capital review after all prior validation. */
    double deploymentConfidenceScore;

    /** True when the catalyst is based on credible objective information rather than hype alone. */
    boolean credibleCatalyst;

    /** True when required source, market-data, and analytics inputs are present. */
    boolean requiredDataPresent;

    /** True when narrative/social propagation is euphoric or fully saturated. */
    boolean euphoricOrSaturated;

    /** True when liquidity, spread, or volatility conditions are hostile enough to block review. */
    boolean hostileMarketStructure;

    /** True when the move appears already fully repriced and asymmetry is gone. */
    boolean equilibriumAlreadyRepriced;

    String notes;

    @Builder(toBuilder = true)
    public CandidateValidationInput(
            String candidateId,
            String symbol,
            double structuralRealityScore,
            double materialSignificanceScore,
            double earlynessScore,
            double equilibriumQualityScore,
            double reflexivityPotentialScore,
            double asymmetryScore,
            double regimeCompatibilityScore,
            double deploymentConfidenceScore,
            boolean credibleCatalyst,
            boolean requiredDataPresent,
            boolean euphoricOrSaturated,
            boolean hostileMarketStructure,
            boolean equilibriumAlreadyRepriced,
            String notes
    ) {
        this.candidateId = Objects.requireNonNull(normalize(candidateId), "candidateId is required");
        this.symbol = Objects.requireNonNull(normalize(symbol), "symbol is required");
        this.notes = notes == null ? "" : notes.trim();
        this.structuralRealityScore = DomainScorePolicy.requireInputScore("structuralRealityScore", structuralRealityScore);
        this.materialSignificanceScore = DomainScorePolicy.requireInputScore("materialSignificanceScore", materialSignificanceScore);
        this.earlynessScore = DomainScorePolicy.requireInputScore("earlynessScore", earlynessScore);
        this.equilibriumQualityScore = DomainScorePolicy.requireInputScore("equilibriumQualityScore", equilibriumQualityScore);
        this.reflexivityPotentialScore = DomainScorePolicy.requireInputScore("reflexivityPotentialScore", reflexivityPotentialScore);
        this.asymmetryScore = DomainScorePolicy.requireInputScore("asymmetryScore", asymmetryScore);
        this.regimeCompatibilityScore = DomainScorePolicy.requireInputScore("regimeCompatibilityScore", regimeCompatibilityScore);
        this.deploymentConfidenceScore = DomainScorePolicy.requireInputScore("deploymentConfidenceScore", deploymentConfidenceScore);
        this.credibleCatalyst = credibleCatalyst;
        this.requiredDataPresent = requiredDataPresent;
        this.euphoricOrSaturated = euphoricOrSaturated;
        this.hostileMarketStructure = hostileMarketStructure;
        this.equilibriumAlreadyRepriced = equilibriumAlreadyRepriced;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
