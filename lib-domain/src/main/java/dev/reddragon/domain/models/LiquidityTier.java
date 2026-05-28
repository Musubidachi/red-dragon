package dev.reddragon.domain.models;

/**
 * Average-volume liquidity buckets used by market-data review surfaces.
 */
public enum LiquidityTier {
    HIGH(1_000_000.0),
    MODERATE(250_000.0),
    ADEQUATE(50_000.0),
    THIN(0.0);

    private final double minimumAverageVolume;

    LiquidityTier(double minimumAverageVolume) {
        this.minimumAverageVolume = minimumAverageVolume;
    }

    public double minimumAverageVolume() {
        return minimumAverageVolume;
    }

    public static LiquidityTier fromAverageVolume(double averageVolume) {
        if (!Double.isFinite(averageVolume)) {
            throw new IllegalArgumentException("averageVolume must be finite");
        }
        if (averageVolume < 0.0) {
            throw new IllegalArgumentException("averageVolume must be non-negative");
        }
        if (averageVolume >= HIGH.minimumAverageVolume) {
            return HIGH;
        }
        if (averageVolume >= MODERATE.minimumAverageVolume) {
            return MODERATE;
        }
        if (averageVolume >= ADEQUATE.minimumAverageVolume) {
            return ADEQUATE;
        }
        return THIN;
    }
}
