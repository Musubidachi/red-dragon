package dev.reddragon.analytics.config;

/**
 * Tunable thresholds for the liquidity and volatility-stability scoring
 * buckets. Moved from {@code lib-marketdata.config} as part of the
 * architectural fix to lib-marketdata REVIEW.md Finding #8 — scoring
 * policy lives at L4 ({@code lib-analytics}), not L2
 * ({@code lib-marketdata}).
 *
 * <p>{@link #defaults()} returns the historical baked-in values that
 * shipped before the move, so an unconfigured deployment is a
 * behavioural no-op.
 */
public class MarketScoringProperties {

    // ---- Liquidity buckets (averageVolume → score) -------------------------
    private final double liquidityVolumeHigh;
    private final double liquidityVolumeMediumHigh;
    private final double liquidityVolumeMedium;
    private final double liquidityVolumeLow;
    private final double liquidityScoreHigh;
    private final double liquidityScoreMediumHigh;
    private final double liquidityScoreMedium;
    private final double liquidityScoreLow;
    private final double liquidityScoreThin;

    // ---- Volatility-stability buckets (ATR% → score) -----------------------
    private final double atrPercentVeryLow;
    private final double atrPercentLow;
    private final double atrPercentMedium;
    private final double atrPercentHigh;
    private final double stabilityScoreVeryHigh;
    private final double stabilityScoreHigh;
    private final double stabilityScoreMedium;
    private final double stabilityScoreLow;
    private final double stabilityScoreVeryLow;
    private final double stabilityUnknownSentinel;

    public MarketScoringProperties(
            double liquidityVolumeHigh,
            double liquidityVolumeMediumHigh,
            double liquidityVolumeMedium,
            double liquidityVolumeLow,
            double liquidityScoreHigh,
            double liquidityScoreMediumHigh,
            double liquidityScoreMedium,
            double liquidityScoreLow,
            double liquidityScoreThin,
            double atrPercentVeryLow,
            double atrPercentLow,
            double atrPercentMedium,
            double atrPercentHigh,
            double stabilityScoreVeryHigh,
            double stabilityScoreHigh,
            double stabilityScoreMedium,
            double stabilityScoreLow,
            double stabilityScoreVeryLow,
            double stabilityUnknownSentinel
    ) {
        if (!(liquidityVolumeHigh > liquidityVolumeMediumHigh
                && liquidityVolumeMediumHigh > liquidityVolumeMedium
                && liquidityVolumeMedium > liquidityVolumeLow
                && liquidityVolumeLow > 0)) {
            throw new IllegalArgumentException(
                    "liquidity volume thresholds must be strictly decreasing and positive");
        }
        if (!(atrPercentVeryLow < atrPercentLow
                && atrPercentLow < atrPercentMedium
                && atrPercentMedium < atrPercentHigh
                && atrPercentVeryLow > 0)) {
            throw new IllegalArgumentException(
                    "ATR% thresholds must be strictly increasing and positive");
        }
        this.liquidityVolumeHigh = liquidityVolumeHigh;
        this.liquidityVolumeMediumHigh = liquidityVolumeMediumHigh;
        this.liquidityVolumeMedium = liquidityVolumeMedium;
        this.liquidityVolumeLow = liquidityVolumeLow;
        this.liquidityScoreHigh = liquidityScoreHigh;
        this.liquidityScoreMediumHigh = liquidityScoreMediumHigh;
        this.liquidityScoreMedium = liquidityScoreMedium;
        this.liquidityScoreLow = liquidityScoreLow;
        this.liquidityScoreThin = liquidityScoreThin;
        this.atrPercentVeryLow = atrPercentVeryLow;
        this.atrPercentLow = atrPercentLow;
        this.atrPercentMedium = atrPercentMedium;
        this.atrPercentHigh = atrPercentHigh;
        this.stabilityScoreVeryHigh = stabilityScoreVeryHigh;
        this.stabilityScoreHigh = stabilityScoreHigh;
        this.stabilityScoreMedium = stabilityScoreMedium;
        this.stabilityScoreLow = stabilityScoreLow;
        this.stabilityScoreVeryLow = stabilityScoreVeryLow;
        this.stabilityUnknownSentinel = stabilityUnknownSentinel;
    }

    /** Historical baked-in defaults; equivalent to the pre-architectural-move behaviour. */
    public static MarketScoringProperties defaults() {
        return new MarketScoringProperties(
                /* liquidityVolumeHigh        */ 5_000_000,
                /* liquidityVolumeMediumHigh  */ 1_000_000,
                /* liquidityVolumeMedium      */   500_000,
                /* liquidityVolumeLow         */   100_000,
                /* liquidityScoreHigh         */ 1.00,
                /* liquidityScoreMediumHigh   */ 0.80,
                /* liquidityScoreMedium       */ 0.60,
                /* liquidityScoreLow          */ 0.35,
                /* liquidityScoreThin         */ 0.15,
                /* atrPercentVeryLow          */ 0.03,
                /* atrPercentLow              */ 0.06,
                /* atrPercentMedium           */ 0.10,
                /* atrPercentHigh             */ 0.15,
                /* stabilityScoreVeryHigh     */ 0.90,
                /* stabilityScoreHigh         */ 0.75,
                /* stabilityScoreMedium       */ 0.55,
                /* stabilityScoreLow          */ 0.35,
                /* stabilityScoreVeryLow      */ 0.15,
                /* stabilityUnknownSentinel   */ 0.50);
    }

    public double getLiquidityVolumeHigh()        { return liquidityVolumeHigh; }
    public double getLiquidityVolumeMediumHigh()  { return liquidityVolumeMediumHigh; }
    public double getLiquidityVolumeMedium()      { return liquidityVolumeMedium; }
    public double getLiquidityVolumeLow()         { return liquidityVolumeLow; }
    public double getLiquidityScoreHigh()         { return liquidityScoreHigh; }
    public double getLiquidityScoreMediumHigh()   { return liquidityScoreMediumHigh; }
    public double getLiquidityScoreMedium()       { return liquidityScoreMedium; }
    public double getLiquidityScoreLow()          { return liquidityScoreLow; }
    public double getLiquidityScoreThin()         { return liquidityScoreThin; }
    public double getAtrPercentVeryLow()          { return atrPercentVeryLow; }
    public double getAtrPercentLow()              { return atrPercentLow; }
    public double getAtrPercentMedium()           { return atrPercentMedium; }
    public double getAtrPercentHigh()             { return atrPercentHigh; }
    public double getStabilityScoreVeryHigh()     { return stabilityScoreVeryHigh; }
    public double getStabilityScoreHigh()         { return stabilityScoreHigh; }
    public double getStabilityScoreMedium()       { return stabilityScoreMedium; }
    public double getStabilityScoreLow()          { return stabilityScoreLow; }
    public double getStabilityScoreVeryLow()      { return stabilityScoreVeryLow; }
    public double getStabilityUnknownSentinel()   { return stabilityUnknownSentinel; }
}
