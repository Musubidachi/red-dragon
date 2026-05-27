package dev.reddragon.analytics.services.classification;

import dev.reddragon.domain.models.RegimeLabel;
import dev.reddragon.domain.models.MarketDataSnapshot;

import java.util.List;
import java.util.Objects;

/**
 * Classifies broad market compatibility from liquidity, volatility, gap, and
 * range behavior. Returns one of seven {@link RegimeLabel} values; the
 * companion {@link #score(RegimeLabel)} maps that label to a {@code [0,1]}
 * compatibility score.
 *
 * <p>This scorer is the source of truth for both the standalone classifier
 * endpoint ({@code POST /api/market-state/classify}) and the candidate
 * pipeline. The thresholds below match {@code DeterministicAnalyticsService}'s
 * (now-deleted) inline classification so the two code paths produce the same
 * label for the same input.
 */
public class RegimeCompatibilityScorer {

    // ---- Classification thresholds -----------------------------------------
    // Gap + volatility combo that flips into news-driven hostile classification.
    private static final double NEWS_DRIVEN_GAP_THRESHOLD        = 0.15;
    private static final double NEWS_DRIVEN_VOLATILITY_THRESHOLD = 0.50;

    // Per-dimension hostile floors.
    private static final double HOSTILE_LIQUIDITY_THRESHOLD      = 0.35;
    private static final double HOSTILE_VOLATILITY_THRESHOLD     = 0.35;

    // Supportive-compression band — tightly balanced structure with healthy backing.
    private static final double COMPRESSION_RANGE_LOWER          = 0.45;
    private static final double COMPRESSION_RANGE_UPPER          = 0.55;
    private static final double COMPRESSION_VOLATILITY_THRESHOLD = 0.70;
    private static final double COMPRESSION_LIQUIDITY_THRESHOLD  = 0.60;

    // Supportive-rotational range band.
    private static final double ROTATIONAL_RANGE_LOWER           = 0.35;
    private static final double ROTATIONAL_RANGE_UPPER           = 0.75;

    // Supportive-trend pressure.
    private static final double TREND_RANGE_THRESHOLD            = 0.75;
    private static final double TREND_VOLATILITY_THRESHOLD       = 0.60;

    // ---- Regime → compatibility score mapping ------------------------------
    private static final double SCORE_SUPPORTIVE_ROTATIONAL      = 0.85;
    private static final double SCORE_SUPPORTIVE_TREND           = 0.70;
    private static final double SCORE_SUPPORTIVE_COMPRESSION     = 0.72;
    private static final double SCORE_MIXED                      = 0.50;
    private static final double SCORE_HOSTILE_NEWS_DRIVEN        = 0.22;
    private static final double SCORE_HOSTILE_VOLATILITY         = 0.25;
    private static final double SCORE_HOSTILE_LIQUIDITY          = 0.20;

    /**
     * Classify the regime from a {@link MarketDataSnapshot}. Each branch is
     * checked in priority order — news-driven hostility wins over generic
     * liquidity/volatility hostility because gap-shock-driven environments
     * have very different equilibrium dynamics; supportive-compression wins
     * over rotational because tight, healthy structure deserves its own
     * label for selective breakout monitoring.
     */
    public RegimeCompatibilityResult process(MarketDataSnapshot marketData) {
        Objects.requireNonNull(marketData, "marketData is required");

        if (newsDriven(marketData)) {
            return new RegimeCompatibilityResult(
                    RegimeLabel.HOSTILE_NEWS_DRIVEN,
                    List.of("Large opening displacement with unstable volatility; treating environment as news-driven and hostile to equilibrium assumptions.")
            );
        }

        if (hostileLiquidity(marketData)) {
            return new RegimeCompatibilityResult(
                    RegimeLabel.HOSTILE_LIQUIDITY,
                    List.of("Liquidity is weak; regime is hostile to concentration.")
            );
        }

        if (hostileVolatility(marketData)) {
            return new RegimeCompatibilityResult(
                    RegimeLabel.HOSTILE_VOLATILITY,
                    List.of("Volatility is unstable; equilibrium behavior is degraded.")
            );
        }

        if (supportiveCompression(marketData)) {
            return new RegimeCompatibilityResult(
                    RegimeLabel.SUPPORTIVE_COMPRESSION,
                    List.of("Structure is tightly balanced with stable volatility and healthy liquidity; compression regime favorable for selective breakout monitoring.")
            );
        }

        if (supportiveRotation(marketData)) {
            return new RegimeCompatibilityResult(
                    RegimeLabel.SUPPORTIVE_ROTATIONAL,
                    List.of("Range position is balanced enough for rotational/restoration behavior.")
            );
        }

        if (supportiveTrend(marketData)) {
            return new RegimeCompatibilityResult(
                    RegimeLabel.SUPPORTIVE_TREND,
                    List.of("Trend pressure is present but volatility remains controlled.")
            );
        }

        return new RegimeCompatibilityResult(
                RegimeLabel.MIXED,
                List.of("Market regime is mixed; no hard support or rejection from market structure alone.")
        );
    }

    /**
     * Compatibility score for a regime label. Exhaustive switch on
     * {@link RegimeLabel}; a new enum value intentionally fails compilation
     * here so the mapping must be reviewed when the framework grows.
     */
    public double score(RegimeLabel regimeLabel) {
        return switch (regimeLabel) {
            case SUPPORTIVE_ROTATIONAL  -> SCORE_SUPPORTIVE_ROTATIONAL;
            case SUPPORTIVE_TREND       -> SCORE_SUPPORTIVE_TREND;
            case SUPPORTIVE_COMPRESSION -> SCORE_SUPPORTIVE_COMPRESSION;
            case MIXED                  -> SCORE_MIXED;
            case HOSTILE_NEWS_DRIVEN    -> SCORE_HOSTILE_NEWS_DRIVEN;
            case HOSTILE_VOLATILITY     -> SCORE_HOSTILE_VOLATILITY;
            case HOSTILE_LIQUIDITY      -> SCORE_HOSTILE_LIQUIDITY;
        };
    }

    private boolean newsDriven(MarketDataSnapshot marketData) {
        return Math.abs(marketData.gapPercent()) > NEWS_DRIVEN_GAP_THRESHOLD
                && marketData.volatilityStabilityScore() < NEWS_DRIVEN_VOLATILITY_THRESHOLD;
    }

    private boolean hostileLiquidity(MarketDataSnapshot marketData) {
        return marketData.liquidityScore() < HOSTILE_LIQUIDITY_THRESHOLD;
    }

    private boolean hostileVolatility(MarketDataSnapshot marketData) {
        return marketData.volatilityStabilityScore() < HOSTILE_VOLATILITY_THRESHOLD;
    }

    private boolean supportiveCompression(MarketDataSnapshot marketData) {
        return marketData.rangePosition() >= COMPRESSION_RANGE_LOWER
                && marketData.rangePosition() <= COMPRESSION_RANGE_UPPER
                && marketData.volatilityStabilityScore() >= COMPRESSION_VOLATILITY_THRESHOLD
                && marketData.liquidityScore() >= COMPRESSION_LIQUIDITY_THRESHOLD;
    }

    private boolean supportiveRotation(MarketDataSnapshot marketData) {
        return marketData.rangePosition() >= ROTATIONAL_RANGE_LOWER
                && marketData.rangePosition() <= ROTATIONAL_RANGE_UPPER;
    }

    private boolean supportiveTrend(MarketDataSnapshot marketData) {
        return marketData.rangePosition() > TREND_RANGE_THRESHOLD
                && marketData.volatilityStabilityScore() >= TREND_VOLATILITY_THRESHOLD;
    }
}
