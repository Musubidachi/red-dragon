/**
 * Ingestion: pull candidate trade inputs from external sources.
 *
 * <p>Source families this module is responsible for:
 * <ul>
 *   <li>SEC filings (8-K, S-1, 13F, Form 4, etc.) via EDGAR</li>
 *   <li>News and press releases via RSS / vendor feeds</li>
 *   <li>Market scanners (relative volume, gaps, breakouts, 52-week highs)</li>
 *   <li>Macro / sector signals (yields, VIX, sector flows, policy calendars)</li>
 * </ul>
 *
 * <p>Each source produces a stream of {@code Candidate} records — a ticker plus
 * the raw evidence that surfaced it. No enrichment, no scoring, no filtering
 * happens here. Downstream modules attach market-data features (lib-marketdata),
 * compute regime/asymmetry scores (lib-analytics), and apply hard rules
 * (lib-validation).
 */
package dev.reddragon.ingestion;
