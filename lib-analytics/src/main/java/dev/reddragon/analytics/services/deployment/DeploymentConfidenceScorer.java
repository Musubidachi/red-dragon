package dev.reddragon.analytics.services.deployment;

import dev.reddragon.analytics.services.ScoreResult;
import dev.reddragon.math.AnalyticsScoreUtils;
import dev.reddragon.domain.models.TradeCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scores whether a setup deserves meaningful capital deployment.
 */
public class DeploymentConfidenceScorer {

    /**
     * Main processing flow.
     */
    public ScoreResult process(
            TradeCandidate candidate,
            double asymmetryScore,
            double equilibriumQualityScore,
            double regimeCompatibilityScore,
            double reflexivityScore
    ) {
        Objects.requireNonNull(candidate, "candidate is required");

        double score = AnalyticsScoreUtils.clamp(
                candidate.structuralRealityScore() * 0.20
                        + candidate.materialSignificanceScore() * 0.15
                        + candidate.earlynessScore() * 0.15
                        + asymmetryScore * 0.20
                        + equilibriumQualityScore * 0.10
                        + regimeCompatibilityScore * 0.10
                        + reflexivityScore * 0.10
        );

        List<String> notes = new ArrayList<>();
        addNotes(score, notes);

        return new ScoreResult(score, notes);
    }

    private void addNotes(double score, List<String> notes) {
        if (score >= 0.85) {
            notes.add("Deployment profile supports concentrated review.");
            return;
        }

        if (score >= 0.65) {
            notes.add("Deployment profile supports standard participation.");
            return;
        }

        if (score >= 0.45) {
            notes.add("Deployment profile supports only limited probing.");
            return;
        }

        notes.add("Deployment profile is weak relative to risk.");
    }
}
