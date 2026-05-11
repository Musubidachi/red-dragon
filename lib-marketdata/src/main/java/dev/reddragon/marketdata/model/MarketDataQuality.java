package dev.reddragon.marketdata.model;

public enum MarketDataQuality {
    COMPLETE,
    INSUFFICIENT_HISTORY,
    STALE,
    ILLIQUID,
    MISSING_SYMBOL,
    EMPTY_BARS
}
