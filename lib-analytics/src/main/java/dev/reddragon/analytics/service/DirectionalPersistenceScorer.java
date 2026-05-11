package dev.reddragon.analytics.service;

import dev.reddragon.analytics.model.IntradayStructureSnapshot;
import dev.reddragon.analytics.util.AnalyticsScoreUtils;

import java.util.List;
import java.util.Objects;

/**
 * Scores whether directional persistence is supportive or hostile.
 */
public class DirectionalPersistenceScorer {

    /**
     * Main processing flow.
     */
    public double process(
            IntradayStructureSnapshot intraday,
            List<String> notes
    ) {
        Objects.requireNonNull(intraday, "intraday is required");
        Objects.requireNonNull(notes, "notes is required");

        double persistence = intraday.directionalPersistenceScore();
        double rotational = intraday.rotationalQualityScore();

        double score = AnalyticsScoreUtils.clamp(
                rotational * 0.65
                        + (1.0 - persistence) * 0.35
        );

        addNote(score, notes);

        return score;
    }

    private void addNote(double score, List<String> notes) {
        if (score >= 0.75) {
            notes.add("Directional persistence remains controlled; rotational behavior is intact.");
            return;
        }

        if (score >= 0.50) {
            notes.add("Directional persistence is elevated but not fully hostile.");
            return;
        }

        notes.add("Directional persistence appears hostile to restoration-style setups.");
    }
}
