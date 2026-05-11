package dev.reddragon.analytics.model;

import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

/**
 * Long-horizon calibration analysis output.
 */
@Value
@Accessors(fluent = true)
public class CalibrationReport {
    CalibrationDriftLevel driftLevel;
    double historicalWinRate;
    double averageReturn;
    double averageDrawdown;
    List<String> findings;
    List<String> recommendations;

    public CalibrationReport(
            CalibrationDriftLevel driftLevel,
            double historicalWinRate,
            double averageReturn,
            double averageDrawdown,
            List<String> findings,
            List<String> recommendations
    ) {
        this.driftLevel = Objects.requireNonNull(driftLevel, "driftLevel is required");
        this.historicalWinRate = historicalWinRate;
        this.averageReturn = averageReturn;
        this.averageDrawdown = averageDrawdown;
        this.findings = List.copyOf(findings == null ? List.of() : findings);
        this.recommendations = List.copyOf(recommendations == null ? List.of() : recommendations);
    }
}
