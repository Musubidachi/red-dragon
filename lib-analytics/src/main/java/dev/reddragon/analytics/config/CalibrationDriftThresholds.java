package dev.reddragon.analytics.config;

/**
 * Tunable drift-band thresholds for long-horizon calibration review.
 *
 * <p>Defaults preserve the historical baked-in behavior. Keep these thresholds
 * in analytics rather than validation: they evaluate realized outcome history,
 * not candidate admission or deployment tiers.
 */
public class CalibrationDriftThresholds {

    private final double stableWinRateMin;
    private final double stableAverageReturnMin;
    private final double stableAverageDrawdownMax;
    private final double minorDriftWinRateMin;
    private final double minorDriftAverageReturnMin;
    private final double moderateDriftWinRateMin;

    public CalibrationDriftThresholds(
            double stableWinRateMin,
            double stableAverageReturnMin,
            double stableAverageDrawdownMax,
            double minorDriftWinRateMin,
            double minorDriftAverageReturnMin,
            double moderateDriftWinRateMin
    ) {
        requireNormalized("stableWinRateMin", stableWinRateMin);
        requireNormalized("stableAverageDrawdownMax", stableAverageDrawdownMax);
        requireNormalized("minorDriftWinRateMin", minorDriftWinRateMin);
        requireNormalized("moderateDriftWinRateMin", moderateDriftWinRateMin);
        requireFinite("stableAverageReturnMin", stableAverageReturnMin);
        requireFinite("minorDriftAverageReturnMin", minorDriftAverageReturnMin);
        if (!(stableWinRateMin > minorDriftWinRateMin
                && minorDriftWinRateMin > moderateDriftWinRateMin)) {
            throw new IllegalArgumentException(
                    "win-rate thresholds must be stable > minor > moderate");
        }
        this.stableWinRateMin = stableWinRateMin;
        this.stableAverageReturnMin = stableAverageReturnMin;
        this.stableAverageDrawdownMax = stableAverageDrawdownMax;
        this.minorDriftWinRateMin = minorDriftWinRateMin;
        this.minorDriftAverageReturnMin = minorDriftAverageReturnMin;
        this.moderateDriftWinRateMin = moderateDriftWinRateMin;
    }

    /** Historical baked-in defaults. */
    public static CalibrationDriftThresholds defaults() {
        return new CalibrationDriftThresholds(
                0.65,
                0.0,
                0.15,
                0.55,
                0.0,
                0.45
        );
    }

    public double getStableWinRateMin() {
        return stableWinRateMin;
    }

    public double getStableAverageReturnMin() {
        return stableAverageReturnMin;
    }

    public double getStableAverageDrawdownMax() {
        return stableAverageDrawdownMax;
    }

    public double getMinorDriftWinRateMin() {
        return minorDriftWinRateMin;
    }

    public double getMinorDriftAverageReturnMin() {
        return minorDriftAverageReturnMin;
    }

    public double getModerateDriftWinRateMin() {
        return moderateDriftWinRateMin;
    }

    private static void requireNormalized(String name, double value) {
        requireFinite(name, value);
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0.0 and 1.0");
        }
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
