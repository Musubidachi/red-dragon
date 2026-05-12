package dev.reddragon.analytics.service;

import dev.reddragon.analytics.model.AdversarialFinding;
import dev.reddragon.analytics.model.PhaseLabel;
import dev.reddragon.analytics.model.TradeModificationAction;
import dev.reddragon.analytics.model.TradeModificationRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Produces safe advisory trade modification requests from changing live context.
 */
public class LiveContextAdaptationAnalyzer {

    /**
     * Main processing flow.
     */
    public TradeModificationRequest process(
            String candidateId,
            String symbol,
            PhaseLabel propagationPhase,
            PhaseLabel equilibriumPhase,
            List<AdversarialFinding> adversarialFindings,
            double deploymentConfidenceScore
    ) {
        Objects.requireNonNull(candidateId, "candidateId is required");
        Objects.requireNonNull(symbol, "symbol is required");
        Objects.requireNonNull(propagationPhase, "propagationPhase is required");
        Objects.requireNonNull(equilibriumPhase, "equilibriumPhase is required");
        Objects.requireNonNull(adversarialFindings, "adversarialFindings are required");

        List<String> reasons = new ArrayList<>();

        double severity = calculateSeverity(
                propagationPhase,
                equilibriumPhase,
                adversarialFindings,
                deploymentConfidenceScore,
                reasons
        );

        TradeModificationAction action = determineAction(severity);

        return new TradeModificationRequest(
                candidateId,
                symbol,
                Instant.now(),
                action,
                severity,
                reasons
        );
    }

    private double calculateSeverity(
            PhaseLabel propagationPhase,
            PhaseLabel equilibriumPhase,
            List<AdversarialFinding> adversarialFindings,
            double deploymentConfidenceScore,
            List<String> reasons
    ) {
        double severity = 0.0;

        if (propagationPhase == PhaseLabel.SATURATION_RISK
                || propagationPhase == PhaseLabel.LATE_REFLEXIVITY) {
            severity += 0.30;
            reasons.add("Propagation phase indicates saturation or late reflexivity.");
        }

        if (equilibriumPhase == PhaseLabel.EQUILIBRIUM_BREAKDOWN
                || equilibriumPhase == PhaseLabel.EXHAUSTION) {
            severity += 0.35;
            reasons.add("Equilibrium quality is deteriorating.");
        }

        if (deploymentConfidenceScore < 0.45) {
            severity += 0.20;
            reasons.add("Deployment confidence materially degraded.");
        }

        double adversarialSeverity = adversarialFindings.stream()
                .mapToDouble(AdversarialFinding::severity)
                .average()
                .orElse(0.0);

        if (adversarialSeverity > 0.0) {
            severity += adversarialSeverity * 0.40;
            reasons.add("Adversarial contradictions are increasing.");
        }

        return Math.max(0.0, Math.min(1.0, severity));
    }

    private TradeModificationAction determineAction(double severity) {
        if (severity >= 0.90) {
            return TradeModificationAction.EXIT_REVIEW;
        }

        if (severity >= 0.75) {
            return TradeModificationAction.REDUCE_EXPOSURE;
        }

        if (severity >= 0.60) {
            return TradeModificationAction.MOVE_TO_PROBE_SIZE;
        }

        if (severity >= 0.45) {
            return TradeModificationAction.TIGHTEN_STOP;
        }

        if (severity >= 0.30) {
            return TradeModificationAction.INCREASE_MONITORING;
        }

        return TradeModificationAction.HOLD;
    }
}
