package dev.reddragon.domain.models;

/**
 * Minimal OHLC contract shared by every bar type that exposes high/low/close.
 *
 * <p>Implemented by {@link MarketBar} (daily bars) and {@link IntradayBar}
 * (sub-day bars) so feature calculators — most notably the ATR family —
 * can operate on either without duplicating the algorithm. See
 * lib-marketdata REVIEW.md Finding #2 for the motivation: two parallel
 * inline ATR implementations had drifted apart, with no compiler
 * pressure to keep them aligned.
 *
 * <p>Method names are fluent (no {@code get*} prefix) to match the
 * project's {@code @Accessors(fluent = true)} convention on
 * {@code @Value} types.
 */
public interface OhlcBar {

    /** Opening price of the bar. Non-negative; zero allowed as a sentinel. */
    double open();

    /** Highest price observed in the bar. {@code >= low()}. */
    double high();

    /** Lowest price observed in the bar. {@code <= high()}. */
    double low();

    /** Closing price of the bar. */
    double close();
}
