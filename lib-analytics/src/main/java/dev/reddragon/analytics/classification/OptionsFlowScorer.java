package dev.reddragon.analytics.classification;

import dev.reddragon.analytics.model.OptionsFlowSnapshot;
import dev.reddragon.analytics.util.AnalyticsScoreUtils;

import java.util.List;
import java.util.Objects;

/**
 * Scores options-flow behavior and dealer positioning pressure.
 */
public class OptionsFlowScorer {

    /**
     * Main processing flow.
     */
    public double process(
            OptionsFlowSnapshot flow,
            List<String> notes
    ) {
        Objects.requireNonNull(flow, "flow is required");
        Objects.requireNonNull(notes, "notes is required");

        double score = AnalyticsScoreUtils.clamp(
                flow.callPutImbalanceScore() * 0.20
                        + flow.unusualActivityScore() * 0.25
                        + flow.openInterestExpansionScore() * 0.20
                        + flow.nearMoneyFlowScore() * 0.20
                        + flow.dealerPressureScore() * 0.15
        );

        addNote(score, notes);

        return score;
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
