package dev.reddragon.analytics.meta;

import dev.reddragon.analytics.model.CalibrationDriftLevel;
import dev.reddragon.analytics.model.CalibrationReport;
import dev.reddragon.analytics.model.OutcomeSample;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates long-horizon framework drift and calibration quality.
 */
public class LongHorizonCalibrationAnalyzer {

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
        if (winRate >= 0.65 && averageReturn > 0.0 && averageDrawdown < 0.15) {
            findings.add("Framework performance remains historically stable.");
            return CalibrationDriftLevel.STABLE;
        }

        if (winRate >= 0.55 && averageReturn >= 0.0) {
            findings.add("Minor degradation detected in framework performance.");
            recommendations.add("Review adversarial thresholds for saturation conditions.");
            return CalibrationDriftLevel.MINOR_DRIFT;
        }

        if (winRate >= 0.45) {
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
