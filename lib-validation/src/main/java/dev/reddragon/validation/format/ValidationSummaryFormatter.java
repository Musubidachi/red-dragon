package dev.reddragon.validation.format;

import dev.reddragon.validation.model.ReasonCode;
import dev.reddragon.validation.model.RiskFlag;
import dev.reddragon.validation.model.ValidationAudit;
import dev.reddragon.validation.model.ValidationResult;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Produces compact summaries and readable reports from validation audits.
 */
public class ValidationSummaryFormatter {

    /**
     * Main processing flow.
     */
    public ValidationSummary process(ValidationAudit audit) {
        Objects.requireNonNull(audit, "audit is required");

        ValidationResult result = audit.validationResult();

        String headline = headline(audit);
        String summaryText = summaryText(audit, result);

        return new ValidationSummary(
                audit.candidateId(),
                audit.symbol(),
                result.verdict(),
                result.deploymentTier(),
                result.score(),
                audit.validationConfidenceScore(),
                audit.riskFlags(),
                headline,
                summaryText
        );
    }

    public String detailedReport(ValidationAudit audit) {
        Objects.requireNonNull(audit, "audit is required");

        ValidationResult result = audit.validationResult();

        StringJoiner joiner = new StringJoiner(System.lineSeparator());

        joiner.add("Validation Report");
        joiner.add("-----------------");
        joiner.add("Candidate: " + audit.symbol());
        joiner.add("Verdict: " + result.verdict());
        joiner.add("Deployment: " + result.deploymentTier());
        joiner.add("Score: " + format(result.score()));
        joiner.add("Confidence: " + format(audit.validationConfidenceScore()));
        joiner.add("");
        joiner.add("Risk Flags: " + riskFlags(audit.riskFlags()));
        joiner.add("Reasons: " + reasons(result.reasonCodes()));
        joiner.add("");
        joiner.add("Explanations:");

        for (String explanation : result.explanations()) {
            joiner.add(" - " + explanation);
        }

        return joiner.toString();
    }

    private String headline(ValidationAudit audit) {
        ValidationResult result = audit.validationResult();

        return audit.symbol()
                + " "
                + result.verdict()
                + " / "
                + result.deploymentTier();
    }

    private String summaryText(
            ValidationAudit audit,
            ValidationResult result
    ) {
        return "Validation score="
                + format(result.score())
                + ", confidence="
                + format(audit.validationConfidenceScore())
                + ", riskFlags="
                + audit.riskFlags().size();
    }

    private String reasons(List<ReasonCode> reasonCodes) {
        StringJoiner joiner = new StringJoiner(", ");

        for (ReasonCode reasonCode : reasonCodes) {
            joiner.add(reasonCode.name());
        }

        return joiner.toString();
    }

    private String riskFlags(List<RiskFlag> riskFlags) {
        if (riskFlags.isEmpty()) {
            return "NONE";
        }

        StringJoiner joiner = new StringJoiner(", ");

        for (RiskFlag riskFlag : riskFlags) {
            joiner.add(riskFlag.name());
        }

        return joiner.toString();
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }
}
