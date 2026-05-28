package dev.reddragon.domain.models;

import dev.reddragon.domain.utilities.DomainScorePolicy;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Rule-based market state signal for discretionary deployment filtering.
 */
@Value
@Accessors(fluent = true)
public class MarketStateSignal {
    RegimeLabel regimeLabel;
    double confidence;
    double equilibriumRestorationProbability;
    boolean deploymentSupported;
    List<String> notes;

    public MarketStateSignal(
            RegimeLabel regimeLabel,
            double confidence,
            double equilibriumRestorationProbability,
            boolean deploymentSupported,
            List<String> notes
    ) {
        this.regimeLabel = regimeLabel == null ? RegimeLabel.MIXED : regimeLabel;
        this.confidence = DomainScorePolicy.clampDerivedScore(confidence);
        this.equilibriumRestorationProbability = DomainScorePolicy.clampDerivedScore(equilibriumRestorationProbability);
        this.deploymentSupported = deploymentSupported;
        this.notes = List.copyOf(notes == null ? List.of() : notes);
    }
}
