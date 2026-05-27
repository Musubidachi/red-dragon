package dev.reddragon.analytics.services.classification;

import dev.reddragon.analytics.services.ScoreResult;
import dev.reddragon.domain.models.IntradayStructureSnapshot;
import dev.reddragon.math.AnalyticsScoreUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scores whether directional persistence is supportive or hostile.
 */
public class DirectionalPersistenceScorer {

    /**
     * Main processing flow.
     */
    public ScoreResult process(IntradayStructureSnapshot intraday) {
        Objects.requireNonNull(intraday, "intraday is required");

        double persistence = intraday.directionalPersistenceScore();
        double rotational = intraday.rotationalQualityScore();

        double score = AnalyticsScoreUtils.clamp(
                rotational * 0.65
                        + (1.0 - persistence) * 0.35
        );

        List<String> notes = new ArrayList<>();
        addNote(score, notes);

        return new ScoreResult(score, notes);
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
