/**
 * SEC EDGAR ingestion adapter.
 *
 * <p>One {@code process()} method per class. Each class has one job. Call
 * order at runtime:
 *
 * <ol>
 *   <li>{@link dev.reddragon.ingestion.sec.SecIngestionService#process(String)}
 *       — given a CIK, returns scored {@code TradeCandidate} records.</li>
 *   <li>{@link dev.reddragon.ingestion.sec.SubmissionsClient#process(String)}
 *       — fetches the SEC submissions JSON via {@link dev.reddragon.ingestion.sec.SecHttpClient}.</li>
 *   <li>{@link dev.reddragon.ingestion.sec.SecHttpClient#process(java.net.URI)}
 *       — blocks on {@link dev.reddragon.ingestion.sec.SimpleRateLimiter} then performs the GET.</li>
 *   <li>{@link dev.reddragon.ingestion.sec.SubmissionsFilingExtractor#process(dev.reddragon.ingestion.sec.SubmissionsResponse)}
 *       — flattens the column-oriented response into {@code SecFiling}s.</li>
 *   <li>{@link dev.reddragon.ingestion.sec.SecCandidateBuilder#process(dev.reddragon.ingestion.sec.SecFiling)}
 *       — produces a fully scored {@code TradeCandidate}.</li>
 * </ol>
 *
 * <p>Configuration lives in {@link dev.reddragon.ingestion.sec.SecApiProperties}.
 * The {@code User-Agent} header must be set to a real contact email before
 * any production use; the SEC rejects requests that look unidentified.
 */
package dev.reddragon.ingestion.sec;
