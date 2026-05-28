package dev.reddragon.domain.models;

import dev.reddragon.domain.utilities.DomainScorePolicy;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Value
@Accessors(fluent = true)
public class AnalyticsSnapshot {
    String candidateId;
    String symbol;
    Instant observedAt;
    RegimeLabel regimeLabel;
    double regimeCompatibilityScore;
    double asymmetryScore;
    double equilibriumQualityScore;
    double reflexivityPotentialScore;
    double deploymentConfidenceScore;
    List<String> reasonNotes;

    public AnalyticsSnapshot(
            String candidateId,
            String symbol,
            Instant observedAt,
            RegimeLabel regimeLabel,
            double regimeCompatibilityScore,
            double asymmetryScore,
            double equilibriumQualityScore,
            double reflexivityPotentialScore,
            double deploymentConfidenceScore,
            List<String> reasonNotes
    ) {
        if (candidateId == null || candidateId.isBlank()) {
            throw new IllegalArgumentException("candidateId is required");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        this.candidateId = candidateId.trim();
        this.symbol = symbol.trim().toUpperCase();
        this.observedAt = Objects.requireNonNull(observedAt, "observedAt is required");
        this.regimeLabel = Objects.requireNonNull(regimeLabel, "regimeLabel is required");
        this.regimeCompatibilityScore = DomainScorePolicy.clampDerivedScore(regimeCompatibilityScore);
        this.asymmetryScore = DomainScorePolicy.clampDerivedScore(asymmetryScore);
        this.equilibriumQualityScore = DomainScorePolicy.clampDerivedScore(equilibriumQualityScore);
        this.reflexivityPotentialScore = DomainScorePolicy.clampDerivedScore(reflexivityPotentialScore);
        this.deploymentConfidenceScore = DomainScorePolicy.clampDerivedScore(deploymentConfidenceScore);
        this.reasonNotes = List.copyOf(reasonNotes == null ? List.of() : reasonNotes);
    }

    /**
     * Returns the dimension with the highest score, useful for
     * one-line summary logging and review display.
     *
     * <p>Each comparison updates both {@code maxScore} and {@code dominant}
     * consistently so that adding a new dimension at the end of the chain
     * does not require special-casing the previous last line.
     */
    public ScoreDimension dominantScore() {
        double maxScore = regimeCompatibilityScore;
        ScoreDimension dominant = ScoreDimension.REGIME_COMPATIBILITY;
        if (asymmetryScore > maxScore)            { maxScore = asymmetryScore;            dominant = ScoreDimension.ASYMMETRY; }
        if (equilibriumQualityScore > maxScore)   { maxScore = equilibriumQualityScore;   dominant = ScoreDimension.EQUILIBRIUM_QUALITY; }
        if (reflexivityPotentialScore > maxScore) { maxScore = reflexivityPotentialScore; dominant = ScoreDimension.REFLEXIVITY_POTENTIAL; }
        if (deploymentConfidenceScore > maxScore) { maxScore = deploymentConfidenceScore; dominant = ScoreDimension.DEPLOYMENT_CONFIDENCE; }
        return dominant;
    }
}
