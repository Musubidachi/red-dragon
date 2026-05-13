package dev.reddragon.marketdata.provider.schwab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Jackson DTO for a single Schwab daily candle. Field names mirror the
 * Schwab API exactly so deserialization is direct.
 *
 * <p>Converted to a normalized {@link dev.reddragon.marketdata.model.MarketBar}
 * before leaving the {@code schwab} package.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SchwabCandle(
        double open,
        double high,
        double low,
        double close,
        long volume,
        long datetime
) {
}
