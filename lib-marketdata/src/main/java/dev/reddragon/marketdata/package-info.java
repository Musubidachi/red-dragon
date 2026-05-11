/**
 * Market data: retrieves daily OHLCV bars and derives features (VWAP proxy, ATR,
 * realized volatility, range position).
 *
 * <p>Provider-agnostic by design — the package exposes a small interface and
 * the concrete provider (Schwab, Polygon, etc.) is wired in {@code app} so swapping
 * sources doesn't ripple through analytics.
 */
package dev.reddragon.marketdata;
