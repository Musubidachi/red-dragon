/**
 * Persistence: JPA entities, Spring Data repositories, Flyway migrations.
 *
 * <h2>MD-layer mapping</h2>
 *
 * Persistence is cross-cutting. It stores the output of every layer that
 * produces a durable artefact:
 * <ul>
 *   <li>MD Layer 1 / 2 — candidate rows (one per ingested
 *       {@link dev.reddragon.domain.models.TradeCandidate})</li>
 *   <li>MD Layer 2 — cached market-data bars</li>
 *   <li>MD Layer 3 / 5 — validation verdicts and deployment tier per candidate</li>
 * </ul>
 *
 * <p><b>For a junior developer:</b> the schema is candidate-in / verdict-out
 * by design. There are no tables for fills, positions, or trade lots — those
 * concerns belong to the future execution module.
 *
 * <p>Anticipated tables:
 * <ul>
 *   <li>{@code candidate} — one row per ingested candidate</li>
 *   <li>{@code validation_verdict} — PASS / WATCH / REJECT plus contributing factors</li>
 *   <li>{@code market_bar} — cached OHLCV bars for tickers we evaluate frequently</li>
 * </ul>
 */
package dev.reddragon.persistence;
