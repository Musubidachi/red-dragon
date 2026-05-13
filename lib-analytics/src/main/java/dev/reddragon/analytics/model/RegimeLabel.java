package dev.reddragon.analytics.model;

/**
 * Describes the broad market-regime context for a candidate.
 *
 * <p>Regimes are classified into supportive, mixed, and hostile categories.
 * Supportive regimes provide tailwinds for disequilibrium resolution;
 * hostile regimes compress asymmetry or introduce structural headwinds.
 */
public enum RegimeLabel {
    SUPPORTIVE_ROTATIONAL,
    SUPPORTIVE_TREND,
    SUPPORTIVE_COMPRESSION,
    MIXED,
    HOSTILE_NEWS_DRIVEN,
    HOSTILE_VOLATILITY,
    HOSTILE_LIQUIDITY;

    /** Human-readable label suitable for display in review surfaces and reports. */
    public String displayName() {
        return switch (this) {
            case SUPPORTIVE_ROTATIONAL -> "Supportive — Rotational";
            case SUPPORTIVE_TREND      -> "Supportive — Trend";
            case SUPPORTIVE_COMPRESSION -> "Supportive — Compression";
            case MIXED                 -> "Mixed";
            case HOSTILE_NEWS_DRIVEN   -> "Hostile — News Driven";
            case HOSTILE_VOLATILITY    -> "Hostile — Volatility";
            case HOSTILE_LIQUIDITY     -> "Hostile — Liquidity";
        };
    }

    /**
     * Returns {@code true} if this regime is broadly supportive for
     * disequilibrium-based setups (i.e., SUPPORTIVE_ROTATIONAL or SUPPORTIVE_TREND).
     */
    public boolean isSupportive() {
        return this == SUPPORTIVE_ROTATIONAL
                || this == SUPPORTIVE_TREND
                || this == SUPPORTIVE_COMPRESSION;
    }
}
