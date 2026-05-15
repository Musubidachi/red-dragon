/**
 * Market data: retrieves OHLCV bars and derives features (VWAP, ATR, realized
 * volatility, range position, liquidity texture, directional persistence).
 *
 * <h2>MD-layer mapping</h2>
 *
 * This module is the <b>market-data half of MD Layer 2 (Data Ingestion
 * Engine)</b>. It is also where the raw features that feed
 * <b>MD Layer 4 (Market-State Classification)</b> in {@code lib-analytics}
 * are computed.
 *
 * <ul>
 *   <li>{@link dev.reddragon.marketdata.services.provider} — provider-agnostic
 *       interface plus concrete adapters (Schwab, Noop). The {@code schwab}
 *       sub-package is the only place that knows the Schwab wire format.</li>
 *   <li>{@link dev.reddragon.marketdata.services} — feature calculators
 *       (VWAP, ATR, realized vol, etc.) and snapshot builders.</li>
 *   <li>{@link dev.reddragon.marketdata.models} — value objects: bars,
 *       order-book snapshots, intraday/liquidity/volatility snapshots.</li>
 *   <li>{@link dev.reddragon.marketdata.services.replay} — frame-based replay for
 *       backtests.</li>
 *   <li>{@link dev.reddragon.marketdata.services.stream} — push-style event surface
 *       for future live data.</li>
 * </ul>
 *
 * <p><b>For a junior developer:</b> the provider is wired in {@code app}, so
 * swapping data sources (Schwab → Polygon → Interactive Brokers) doesn't ripple
 * through analytics or validation.
 */
package dev.reddragon.marketdata;
