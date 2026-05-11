/**
 * Persistence: JPA entities, Spring Data repositories, Flyway migrations.
 *
 * <p>Schema is centered on the opportunity pipeline, not on portfolio history.
 * Anticipated tables:
 * <ul>
 *   <li>{@code candidate} — one row per ingested candidate (ticker + source + raw evidence)</li>
 *   <li>{@code enrichment_snapshot} — market-data features attached to a candidate at a point in time</li>
 *   <li>{@code regime_snapshot} — the governing market state at the time of evaluation</li>
 *   <li>{@code validation_verdict} — PASS / WATCH / REJECT plus contributing factors</li>
 *   <li>{@code market_bar} — cached EOD OHLCV bars for tickers we evaluate frequently</li>
 * </ul>
 *
 * <p>No tables for fills, positions, or trade lots. The system is candidate-in /
 * verdict-out by design.
 */
package dev.reddragon.persistence;
