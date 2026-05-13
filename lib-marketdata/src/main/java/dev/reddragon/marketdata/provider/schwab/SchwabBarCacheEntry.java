package dev.reddragon.marketdata.provider.schwab;

import dev.reddragon.marketdata.model.MarketBar;

import java.time.Instant;
import java.util.List;

/**
 * Cached batch of Schwab daily bars for a given symbol/date range, with an
 * expiry helper.
 *
 * <p>Extracted from {@code SchwabMarketDataProvider} so the cache record is
 * not buried as a private nested class inside the provider.
 */
record SchwabBarCacheEntry(Instant storedAt, List<MarketBar> bars) {

    /**
     * @param ttlSeconds the cache time-to-live; zero or negative disables caching
     * @return true if this entry is older than {@code ttlSeconds} (i.e. should be re-fetched)
     */
    boolean expired(long ttlSeconds) {
        return ttlSeconds <= 0 || Instant.now().isAfter(storedAt.plusSeconds(ttlSeconds));
    }
}
