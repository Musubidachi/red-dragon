package dev.reddragon.analytics.services.meta;

import dev.reddragon.analytics.config.CalibrationDriftThresholds;
import dev.reddragon.domain.models.CalibrationDriftLevel;
import dev.reddragon.domain.models.CalibrationReport;
import dev.reddragon.domain.models.OutcomeSample;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates long-horizon framework drift and calibration quality.
 */
public class LongHorizonCalibrationAnalyzer {

    // ---- Drift-level thresholds --------------------------------------------
    // Configurable drift thresholds; defaults preserve the historical mapping.
    // These stay analytics-owned because they evaluate realized outcomes,
    // not validation admission or deployment-tier decisions.
    private final CalibrationDriftThresholds thresholds;

    public LongHorizonCalibrationAnalyzer() {
        this(CalibrationDriftThresholds.defaults());
    }

    public LongHorizonCalibrationAnalyzer(CalibrationDriftThresholds thresholds) {
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds are required");
    }

    /**
     * Main processing flow.
     */
    public CalibrationReport process(List<OutcomeSample> samples) {
        Objects.requireNonNull(samples, "samples are required");

        if (samples.isEmpty()) {
            return emptyReport();
        }

        double winRate = calculateWinRate(samples);
        double averageReturn = calculateAverageReturn(samples);
        double averageDrawdown = calculateAverageDrawdown(samples);

        List<String> findings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        CalibrationDriftLevel driftLevel = determineDrift(
                winRate,
                averageReturn,
                averageDrawdown,
                findings,
                recommendations
        );

        return new CalibrationReport(
                driftLevel,
                winRate,
                averageReturn,
                averageDrawdown,
                findings,
                recommendations
        );
    }

    private CalibrationReport emptyReport() {
        return new CalibrationReport(
                CalibrationDriftLevel.STABLE,
                0.0,
                0.0,
                0.0,
                List.of("No historical samples available."),
                List.of("Collect more outcome samples before calibration review.")
        );
    }

    private double calculateWinRate(List<OutcomeSample> samples) {
        long wins = samples.stream()
                .filter(OutcomeSample::thesisWorked)
                .count();

        return (double) wins / samples.size();
    }

    private double calculateAverageReturn(List<OutcomeSample> samples) {
        return samples.stream()
                .mapToDouble(OutcomeSample::realizedReturn)
                .average()
                .orElse(0.0);
    }

    private double calculateAverageDrawdown(List<OutcomeSample> samples) {
        return samples.stream()
                .mapToDouble(OutcomeSample::maxDrawdown)
                .average()
                .orElse(0.0);
    }

    private CalibrationDriftLevel determineDrift(
            double winRate,
            double averageReturn,
            double averageDrawdown,
            List<String> findings,
            List<String> recommendations
    ) {
        if (winRate >= thresholds.getStableWinRateMin()
                && averageReturn > thresholds.getStableAverageReturnMin()
                && averageDrawdown < thresholds.getStableAverageDrawdownMax()) {
            findings.add("Framework performance remains historically stable.");
            return CalibrationDriftLevel.STABLE;
        }

        if (winRate >= thresholds.getMinorDriftWinRateMin()
                && averageReturn >= thresholds.getMinorDriftAverageReturnMin()) {
            findings.add("Minor degradation detected in framework performance.");
            recommendations.add("Review adversarial thresholds for saturation conditions.");
            return CalibrationDriftLevel.MINOR_DRIFT;
        }

        if (winRate >= thresholds.getModerateDriftWinRateMin()) {
            findings.add("Moderate framework drift detected.");
            recommendations.add("Reevaluate propagation and asymmetry assumptions.");
            recommendations.add("Tighten deployment concentration thresholds.");
            return CalibrationDriftLevel.MODERATE_DRIFT;
        }

        findings.add("Major framework drift detected.");
        recommendations.add("Reduce deployment aggressiveness.");
        recommendations.add("Reevaluate structural reality validation assumptions.");
        recommendations.add("Investigate whether regime behavior fundamentally changed.");

        return CalibrationDriftLevel.MAJOR_DRIFT;
    }
}
