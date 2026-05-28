package dev.reddragon.analytics.services.classification;

import dev.reddragon.analytics.services.ScoreResult;
import dev.reddragon.domain.models.OptionsFlowSnapshot;
import dev.reddragon.math.AnalyticsScoreUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scores options-flow behavior and dealer positioning pressure.
 */
public class OptionsFlowScorer {

    /**
     * Main processing flow.
     */
    public ScoreResult process(OptionsFlowSnapshot flow) {
        Objects.requireNonNull(flow, "flow is required");

        double score = AnalyticsScoreUtils.clamp(
                flow.callPutImbalanceScore() * 0.20
                        + flow.unusualActivityScore() * 0.25
                        + flow.openInterestExpansionScore() * 0.20
                        + flow.nearMoneyFlowScore() * 0.20
                        + flow.dealerPressureScore() * 0.15
        );

        List<String> notes = new ArrayList<>();
        addNote(score, notes);

        return new ScoreResult(score, notes);
    }

    private void addNote(double score, List<String> notes) {
        if (score >= 0.80) {
            notes.add("Options flow appears supportive and potentially reflexive.");
            return;
        }

        if (score >= 0.55) {
            notes.add("Options flow is moderately supportive.");
            return;
        }

        notes.add("Options flow appears weak, fragmented, or unsupportive.");
    }
}
