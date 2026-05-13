package dev.reddragon.marketdata.model;

/**
 * Describes the completeness and reliability of market data for a given symbol.
 */
public enum MarketDataQuality {
    COMPLETE,
    INSUFFICIENT_HISTORY,
    STALE,
    ILLIQUID,
    MISSING_SYMBOL,
    EMPTY_BARS;

    /** Human-readable label suitable for display in review surfaces and reports. */
    public String displayName() {
        return switch (this) {
            case COMPLETE             -> "Complete";
            case INSUFFICIENT_HISTORY -> "Insufficient History";
            case STALE                -> "Stale";
            case ILLIQUID             -> "Illiquid";
            case MISSING_SYMBOL       -> "Missing Symbol";
            case EMPTY_BARS           -> "Empty Bars";
        };
    }
}
