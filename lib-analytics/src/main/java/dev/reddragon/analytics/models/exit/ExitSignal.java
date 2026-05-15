package dev.reddragon.analytics.models.exit;

import dev.reddragon.analytics.utilities.AnalyticsScoreUtils;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

/**
 * Output of the L7 (Exit / Equilibrium Compression) layer.
 *
 * <p>Carries a recommended action, a 0.0–1.0 <b>compression score</b> (how much
 * of the asymmetric edge has been used up), and an ordered list of notes the
 * trader can read to understand why the system recommends what it does.
 *
 * <p>The recommendation is advisory — final exit decisions remain
 * discretionary.
 */
@Value
@Accessors(fluent = true)
public class ExitSignal {

    /** Suggested next action: HOLD / TIGHTEN / SCALE_OUT / EXIT_NOW. */
    ExitRecommendation recommendation;

    /**
     * How compressed the original asymmetry has become, 0.0 (untouched) –
     * 1.0 (entirely consumed). Driven primarily by current-vs-entry asymmetry
     * but amplified by propagation saturation and range extension.
     */
    double compressionScore;

    /** Ordered human-readable explanation. Each note maps to one signal. */
    List<String> notes;

    public ExitSignal(
            ExitRecommendation recommendation,
            double compressionScore,
            List<String> notes
    ) {
        this.recommendation = Objects.requireNonNull(recommendation, "recommendation is required");
        this.compressionScore = AnalyticsScoreUtils.clamp(compressionScore);
        this.notes = List.copyOf(notes == null ? List.of() : notes);
    }
}
