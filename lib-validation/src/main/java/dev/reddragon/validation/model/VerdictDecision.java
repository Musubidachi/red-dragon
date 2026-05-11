package dev.reddragon.validation.model;

import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

/**
 * Result of resolving the validation verdict before deployment tier selection.
 */
@Value
@Accessors(fluent = true)
public class VerdictDecision {
    Verdict verdict;
    List<ReasonCode> reasonCodes;
    List<String> explanations;

    public VerdictDecision(
            Verdict verdict,
            List<ReasonCode> reasonCodes,
            List<String> explanations
    ) {
        this.verdict = Objects.requireNonNull(verdict, "verdict is required");
        this.reasonCodes = List.copyOf(reasonCodes == null ? List.of() : reasonCodes);
        this.explanations = List.copyOf(explanations == null ? List.of() : explanations);
    }
}
