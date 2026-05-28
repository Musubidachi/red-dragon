package dev.reddragon.math;

/**
 * Package-private primitive implementations shared by the public lib-math
 * facades. Keeping this internal preserves the existing API while avoiding
 * duplicated arithmetic and sentinel behavior.
 */
final class CoreMathUtils {

    private CoreMathUtils() {
    }

    static double clamp(double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("clamp: value must not be NaN");
        }
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    static double average(double left, double right) {
        return (left + right) / 2.0;
    }

    static double weightedAverage(double weightedTotal, double totalWeight) {
        return totalWeight == 0.0 ? 0.0 : weightedTotal / totalWeight;
    }
}
