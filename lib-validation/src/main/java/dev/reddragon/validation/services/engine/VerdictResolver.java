package dev.reddragon.validation.services.engine;

import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.validation.models.ReasonCode;
import dev.reddragon.validation.models.Verdict;
import dev.reddragon.validation.models.VerdictDecision;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves the final validation verdict from score and hard-gate state.
 */
public class VerdictResolver {

    private final ValidationThresholds thresholds;

    public VerdictResolver(ValidationThresholds thresholds) {
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds is required");
    }

    /**
     * Main processing flow.
     */
    public VerdictDecision process(
            double score,
            List<ReasonCode> hardGateFailures
    ) {
        List<ReasonCode> reasons = new ArrayList<>();
        List<String> explanations = new ArrayList<>();

        if (!hardGateFailures.isEmpty()) {
            reasons.addAll(hardGateFailures);
            explanations.add("Rejected because one or more hard gates failed before scoring could justify review.");
            return new VerdictDecision(Verdict.REJECT, reasons, explanations);
        }

        if (score >= thresholds.passThreshold()) {
            explanations.add("Passed: evidence supports a real, meaningful, early disequilibrium with favorable remaining asymmetry.");
            return new VerdictDecision(Verdict.PASS, reasons, explanations);
        }

        if (score >= thresholds.watchThreshold()) {
            explanations.add("Watch: candidate has a plausible disequilibrium, but confirmation or asymmetry is not strong enough for a pass.");
            return new VerdictDecision(Verdict.WATCH, reasons, explanations);
        }

        reasons.add(ReasonCode.SCORE_BELOW_THRESHOLD);
        explanations.add("Rejected because the aggregate validation score is below the watch threshold.");

        return new VerdictDecision(Verdict.REJECT, reasons, explanations);
    }
}
