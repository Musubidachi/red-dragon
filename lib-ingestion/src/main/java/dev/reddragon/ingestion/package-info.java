/**
 * Ingestion: pull candidate trade inputs from external sources.
 *
 * <h2>MD-layer mapping</h2>
 *
 * This module implements the first two layers of the trading framework spec:
 *
 * <ul>
 *   <li><b>MD Layer 1 — Opportunity Discovery Engine</b>: surface possible
 *       opportunities from SEC EDGAR filings, manual entry, and (future) RSS /
 *       news / sector scanners. Lives in {@link dev.reddragon.ingestion.services.sec}
 *       and {@link dev.reddragon.ingestion.services}.</li>
 *   <li><b>MD Layer 2 — Data Ingestion Engine</b>: normalize each discovered
 *       item into a {@link dev.reddragon.ingestion.models.TradeCandidate} —
 *       timestamping, source tracking, entity extraction, metadata tagging.</li>
 * </ul>
 *
 * <p><b>For a junior developer:</b> the boundary contract is simple — every
 * source produces {@link dev.reddragon.ingestion.models.TradeCandidate}s. No
 * enrichment, no scoring, no filtering happens here. Downstream modules
 * attach market-data features (lib-marketdata), score them (lib-analytics),
 * and apply hard rules (lib-validation).
 */
package dev.reddragon.ingestion;
